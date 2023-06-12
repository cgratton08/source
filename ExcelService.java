package com.desjardins.marchecapitaux.niad.security.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.desjardins.marchecapitaux.niad.security.config.ConfigDataSmart;
import com.desjardins.marchecapitaux.niad.security.dto.JsonResponse;
import com.desjardins.marchecapitaux.niad.security.dto.SmartdEntity;
import com.desjardins.marchecapitaux.niad.security.exception.NiadSecurityException;
import com.desjardins.marchecapitaux.niad.security.exception.SmartdException;
import com.desjardins.marchecapitaux.niad.security.utils.JsonResponseStatus;
import com.desjardins.marchecapitaux.niad.security.utils.TypeClient;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class ExcelService 
{
	private static final String ACCESS_TOKEN = "access_token";
	private static final String BEARER = "Bearer ";
	
	private static final String TOKEN_EXCEPTION_MSG = "Error retrieving JWT from PISE token is null or empty";
	
	private final WebClient.Builder webClientBuilder;
	private final ConfigDataSmart configDataSmart;	
		
	private static final String ERR_FILES_NOT_FOUND = "error.files.notfound";	
	private static final String ERR_ACCOUNTS_NOT_FOUND = "error.accounts.notfound";
	private static final String ERR_RETRIEVING_NAS = "error.retrieving.nas";
	private static final String ERR_VALIDATING_ENTITIES = "error.validating.entities";
	private static final String ERR_VALIDATING_ENTITIES_NUMBER = "error.validating.entities.number";
	private static final String ERR_EXECUTING_APPLICATON = "error.executing.application";
	private static final String ERR_MOVIN_FILE_TO_DESTINATION = "error.moving.file.destination";
	private static final String SUCCESS_PROCESSING_FILE = "success.processing.file";
	
	@Value("${smart.dir.input}")
	private String inputDir;
	
	@Value("${smart.dir.output}")
	private String outputDir;
	
	@Value("${smart.workbook.initalRow}")
	private Integer initialRow;

	@Value("${smart.workbook.accountCell}")
	private Integer accountCell;

	@Value("${smart.workbook.nasCell}")
	private Integer nasCell;

	@Value("${smart.workbook.blankCell}")
	private List<String> blankCell;
	
	@Autowired
	FileDirectoryService fileDirSrv;
	
	@Autowired
	MailService mailSrv;
	
	@Autowired
	SecurityService securitySrv;
	
	@Autowired
	MessageSource messageSource;

	public ExcelService(WebClient.Builder webClientBuilder, ConfigDataSmart configDataSmart) {
		this.webClientBuilder = webClientBuilder;
		this.configDataSmart = configDataSmart;
	}
			
	/**
	 * Parse excel file to retrieve accounts number
	 * Call SmartD to get associated NAS number for each accounts
	 * Validate return result from SmartD
	 * Update excel file with NAS number
	 * 
	 * @param files
	 * @param dir
	 * @throws IOException
	 * @throws NiadSecurityException
	 */
	public void processExcelFile(List<File> files, String dir) throws IOException, NiadSecurityException
	{		
		if (files == null || files.isEmpty()) {			
			sendErrorMessage(messageSource.getMessage(ERR_FILES_NOT_FOUND, new String[]{dir}, Locale.CANADA_FRENCH));
			return;
		}
						
		// For each files found in dir. Note for the moment only one file should be deposited in the directory  
		for (File file : files)
		{	
			// Create the virtual workbook to edit the excel file
			try (Workbook workbook = WorkbookFactory.create(new FileInputStream(file)); FileOutputStream outputStream = new FileOutputStream(file);)
			{	
				log.info("Collecting accounts numbers from excel file [{}]", file.getPath());
				List<String> accounts = fetchAccounNumbersFromExcelFile(workbook);
				if (accounts.isEmpty()) {
					terminate(messageSource.getMessage(ERR_ACCOUNTS_NOT_FOUND, new String[]{file.getName(), dir}, Locale.CANADA_FRENCH), workbook, outputStream, file, false);
					return;
				}
				log.info("Found [{}] accounts from excel file [{}]", accounts.size(), file.getPath());
								
				log.info("Collecting NAS from SmartD for account {}", accounts);
				JsonResponse jsonResponse = fetchNasFromSmartd(accounts).block();				
				if (jsonResponse == null || jsonResponse.getStatus() == null && !jsonResponse.getStatus().equals(JsonResponseStatus.OK)) {				
					terminate(messageSource.getMessage(ERR_RETRIEVING_NAS, new String[] {file.getName(), dir, jsonResponse.getStatus().toString()}, Locale.CANADA_FRENCH), workbook, outputStream, file, false);
					return;
				}
							
				// Validate the return PayLoad from Smart. 
				// Is all the accounts send to Smart were valid and were return by Smart ? 
				// Does all the entities return of type PARTICULIER have an associated NAS 
				// Doublons found in the Excel files are not return in double by Smart and are ignored by the prg.
				log.info("Validating entities return by SmartD");
				List<SmartdEntity>	entities = jsonResponse.getPayload();
				if (entities == null || entities.isEmpty()) {					
					terminate(messageSource.getMessage(ERR_VALIDATING_ENTITIES, new String[] {file.getName(), dir}, Locale.CANADA_FRENCH), workbook, outputStream, file, false);
					return;
					
				} else if (accounts.size() != entities.size()) {  
				 
					List<String> accountsNotValid = isSmartdReturnEntitiesComplete(accounts, entities);
					terminate(messageSource.getMessage(ERR_VALIDATING_ENTITIES_NUMBER, new Object[] {file.getName(), dir, accountsNotValid}, Locale.CANADA_FRENCH), workbook, outputStream, file, false);
					return;
									
				} if ( !isSmartdReturnEntitiesValid(entities)) {
				
					terminate(messageSource.getMessage(ERR_VALIDATING_ENTITIES, new String[] {file.getName(), dir}, Locale.CANADA_FRENCH), workbook, outputStream, file, false);
					return;
				}				
				
				// Update excel file with result
				log.info("Updating excel file with info return by SmartD");
				updateWorkbookWithNas(entities, file, workbook.getSheetAt(0));

				// Clean excel file of content for specific cell for security reason
				log.info("Cleaning cell content from sensitive values for security reason");
				cleanWorkbookCell(workbook.getSheetAt(0));
				
				// All the rows have been process successfully, save the workbook
				log.info("Saving excel file");
				workbook.write(outputStream);														
			}
			catch (Exception e) {								
				terminate(messageSource.getMessage(ERR_EXECUTING_APPLICATON, new String[]{file.getName(), dir, e.getMessage()}, Locale.CANADA_FRENCH), null, null, file, true);
				return;
			}
						
			// Move the file to the output dir. This must happen after the stream are close from the try with resources
			Path outputPath = Paths.get(outputDir + File.separator + dir + File.separator + file.getName());			
			if (!fileDirSrv.moveFile(file.toPath(), outputPath))								
				sendErrorMessage(messageSource.getMessage(ERR_MOVIN_FILE_TO_DESTINATION, new String[] {file.getName(), outputPath.toString()}, Locale.CANADA_FRENCH));							
			
			// send mail with success message
			sendSuccessMessage(messageSource.getMessage(SUCCESS_PROCESSING_FILE, new String[] {file.getName(), dir}, Locale.CANADA_FRENCH));
		}
	}
	
	/**
	 * Fetch accounts number from the excel file
	 * The row containing account number start on row (initialRow ==> 14) configured in the yaml file
	 * The cell containing account info is on cell (accountCell ==> 14) configured in the yaml file
	 * 
	 * @param workbook
	 * @return
	 * @throws Exception
	 */
	protected List<String> fetchAccounNumbersFromExcelFile(Workbook workbook) throws Exception
	{
		List<String> accounts = new ArrayList<>();
		
		// Sheet can not be null. At least one sheet need to be present in the excel file
		Sheet sheet = workbook.getSheetAt(0);
		for (int i = initialRow; ; i++) 
		{
			Row row = sheet.getRow(i);
			String cellValue = row.getCell(accountCell).getStringCellValue();
			
			// Terminate file parsing when the cell is empty, meaning no more row to read
			if (!StringUtils.hasText(cellValue))
				break;
			
			// The account cell display info on 3 lines. The account number is on the first line
			accounts.add(cellValue.lines().findFirst().get());
		}	
		
		return  accounts;
	}
	
	/**
	 * Fetch the NAS from SmartD micro service
	 * 
	 * @param accounts
	 * @return
	 * @throws SmartdException
	 * @throws Exception
	 */
	protected Mono<JsonResponse> fetchNasFromSmartd(List<String> accounts) throws SmartdException, Exception
	{
		// Pick up token
		String token = getToken();
		if (!StringUtils.hasText(token))
			throw new SmartdException("Token return is null or empty");

		log.info("JWT Token retrieve: {}...", token.substring(0, 10));
		
		webClientBuilder.baseUrl(configDataSmart.getQuery().getBaseUrl());
		return webClientBuilder
		.build()
		.post()
		.uri(uriBuilder -> uriBuilder.path(configDataSmart.getQuery().getUri())				
		.build())
		.header(HttpHeaders.AUTHORIZATION, BEARER + token)
		.body(BodyInserters.fromValue(accounts))			
		.accept(MediaType.valueOf(configDataSmart.getQuery().getAccept()))
        .retrieve()
		.onStatus(HttpStatus::is4xxClientError, cr -> Mono.just(new SmartdException(cr.statusCode().getReasonPhrase())))
		.onStatus(HttpStatus::is5xxServerError, cr -> Mono.just(new SmartdException(cr.statusCode().getReasonPhrase())))
		.bodyToMono(JsonResponse.class);
	}	
		
	protected String getToken() throws SmartdException, Exception
	{
		String token = null;
		try
		{
			JSONObject jsonObject = new JSONObject(securitySrv.getSmartJwt().block());
			token = jsonObject.getString(ACCESS_TOKEN);
			
			if (!StringUtils.hasText(token))
				throw new SmartdException(TOKEN_EXCEPTION_MSG);
			
		} catch (Exception jse) {
			throw new SmartdException(TOKEN_EXCEPTION_MSG);
		}
		
		return token;
	}
	
	/**
	 * Validate the return PayLoad from Smart.
	 * Is all the accounts send to Smart were valid and were return by Smart ?
	 * Does all the entities return of type PARTICULIER have an associated NAS
	 * 
	 * @param accounts
	 * @param entities
	 * @return
	 */
	protected List<String> isSmartdReturnEntitiesComplete(List<String> accounts, List<SmartdEntity> entities)
	{
		// If an account send to Smart is not valid Smart does not return this account. it just ignore it.
		// So the only way to validate the accounts found in the Excel file is to check if Smart return all of them.
		List<String> accountsNotValid = new ArrayList<>();
		
		// Does each accounts can be found in the return Payload from Smart 
		for (String account : accounts)
		{
			boolean notFound = true;
			for (SmartdEntity entity : entities)
			{				
				if (entity.getNumeroCompte().equalsIgnoreCase(account))	{							
					notFound = false;	
					continue;
				}
			}
			
			if (notFound)
				accountsNotValid.add(account);
		}
		
		if (accountsNotValid.size() > 0) {
			log.error("Error when validating entities return by SmartD");
			log.error("Those accounts [{}] were not return by Smart", accountsNotValid);
		}
		
		return accountsNotValid;
	}
	
	/**
	 * Validate the result return by SmartD micro service
	 * 
	 * @param entities
	 * @return
	 */
	protected boolean isSmartdReturnEntitiesValid(List<SmartdEntity> entities)
	{
		// Validate excel file each typeClient.PARTICULIER must have a NAS
		for (SmartdEntity entity : entities)
		{
			// Validate Result
			if (entity.getTypeClient() == TypeClient.PARTICULIER && !StringUtils.hasText(entity.getNumeroNAS())) {
				log.error("Error when validating entities return by SmartD");
				log.error("Could not retrieve NAS from account [{}] program will terminate", entity.getNumeroCompte());
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Update excel file accounts with their respective NAS
	 * 
	 * @param entities
	 * @param file
	 * @param sheet
	 * @throws Exception
	 */
	protected void updateWorkbookWithNas(List<SmartdEntity> entities, File file, Sheet sheet) throws Exception
	{
		// For each entity return by SmartD
		for (SmartdEntity entity : entities)
		{
			if (entity.getTypeClient() == TypeClient.PARTICULIER) 
			{			
				// Search for the corresponding account in the excel file  
				for (int i = initialRow; ; i++) 
				{
					Row row = sheet.getRow(i);
					String cellValue = row.getCell(accountCell).getStringCellValue();
					
					// No more value to read in the excel sheet
					if (!StringUtils.hasText(cellValue))
						break;
					
					// Set the NAS for this account
					if (StringUtils.hasText(cellValue) && cellValue.lines().findFirst().get().equalsIgnoreCase(entity.getNumeroCompte())) {
						row.getCell(nasCell).setCellValue(entity.getNumeroNAS());
						log.debug("Updating excel sheet [{}] accountNbr [{}] with NAS [{}]", file.getName(), entity.getNumeroCompte(), entity.getNumeroNAS());
						break;
					}
				}
			}
			else if (entity.getTypeClient() == TypeClient.ENTREPRISE)
				log.info("Client with account [{}] is of type Enterprise nothing to do", entity.getNumeroCompte());
		}
	}
	
	/**
	 * Clean workbook from any sensitive data as define by the client
	 * 
	 * @param sheet
	 * @throws Exception
	 */
	protected void cleanWorkbookCell(Sheet sheet) throws Exception
	{
		for (int i = initialRow - 1; ; i++) 
		{
			Row row = sheet.getRow(i);
			String cellValue = row.getCell(accountCell).getStringCellValue();
			
			// No more value to read in the excel sheet
			if (!StringUtils.hasText(cellValue))
				break;
			
			for(String cell : blankCell)
			{
				CellReference cr = new CellReference(cell + row.getRowNum());				
				row.getCell(cr.getCol()).setCellValue("");				
			}
		}
	}
	
	protected void terminate(String message, Workbook workbook, FileOutputStream outputStream, File file, boolean deleteFileOnly) throws IOException 
	{
		log.error(message);
		closeStreamDeleteFile(workbook, outputStream, file, deleteFileOnly);		
		sendErrorMessage(message);
	}
	
	protected void closeStreamDeleteFile(Workbook workbook, FileOutputStream outputStream, File file, boolean deleteFileOnly) throws IOException
	{
		if (workbook != null && outputStream != null && !deleteFileOnly) {
			workbook.write(outputStream);
			workbook.close();
		} else if (workbook != null && !deleteFileOnly) {
			workbook.close();
		}
					
		if (outputStream != null && !deleteFileOnly)
			outputStream.close();
		
		if (file != null)
			fileDirSrv.deleteFile(file);
		
	}

	protected void sendSuccessMessage(String message)
	{
		if (!StringUtils.hasText(message)) 
			log.error("Error sending success message. The message is null");
				
		log.info(message);
		sendMessage(message);
	}
	
	protected void sendErrorMessage(String message)
	{
		if (!StringUtils.hasText(message)) 
			log.error("Error sending error message. The message is null");
		
		log.error(message);
		sendMessage(message);
	}

	protected void sendMessage(String message)
	{
		try {
			JSONObject jsonObject = new JSONObject(mailSrv.sendMail(message).block());
			log.info("Response {}", jsonObject);
		} catch (Exception e) {
			log.error("Error sending message {}", e);
		}		
	}
}
