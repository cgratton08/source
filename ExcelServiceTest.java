package com.desjardins.marchecapitaux.niad.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import com.desjardins.marchecapitaux.niad.security.BaseTest;
import com.desjardins.marchecapitaux.niad.security.config.ConfigDataSmart;
import com.desjardins.marchecapitaux.niad.security.config.ConfigDataSmart.Query;
import com.desjardins.marchecapitaux.niad.security.dto.JsonResponse;
import com.desjardins.marchecapitaux.niad.security.dto.SmartdEntity;
import com.desjardins.marchecapitaux.niad.security.exception.SmartdException;
import com.desjardins.marchecapitaux.niad.security.utils.JsonResponseStatus;

import reactor.core.publisher.Mono;


@TestMethodOrder(OrderAnnotation.class)
@TestInstance(Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
public class ExcelServiceTest extends BaseTest 
{
	// @Mock : This annotation creates an instance of the dummy implementation of the class.
	// @Spy: This annotation is used to stub an class with its actual implementation.
	// @InjectMocks: If a class has dependency to some other classes, then in order to Mock that class we need to use @InjectMocks annotation

	ExcelService excelSrvSpy;

	@Mock
	FileDirectoryService fileDirSrvMock;

	@Mock
	SecurityService securitySrvMock;
	
	@Mock
	MailService mailSrvMock;		

	@Mock
	MessageSource messageSourceMock;
	
	@Mock
	WebClient.Builder clientBuilderMock;

	@Mock
	ConfigDataSmart configDataSmartMock;

	// Web Client
	@Mock
	WebClient webClientMock;
	
	@Mock
	Query queryMock;
	
	@SuppressWarnings("rawtypes")
	@Mock
	Mono monoMock;

	@Mock
	RequestBodyUriSpec requestBodyUriSpecMock;
	
	@Mock
	RequestBodySpec requestBodySpecMock;
	
	@Mock
	ResponseSpec responseSpecMock;
	
	@Mock
	Workbook workbookMock;
	
	public List<SmartdEntity> listParticulier = new ArrayList<>();
	public List<SmartdEntity> listParticulierWithErrors = new ArrayList<>();
	public List<SmartdEntity> listEntreprise = new ArrayList<>();
	
	public JsonResponse jsonResponseEmpty = new JsonResponse();
	public JsonResponse jsonResponseParticulier = new JsonResponse();
	public JsonResponse jsonResponseEntreprise = new JsonResponse();
	public JsonResponse jsonResponseParticulierWithErrors = new JsonResponse();
	
	Workbook workbook = null;
	
		
	@BeforeAll
	void setUpBeforeClass()
	{
		classLoader = getClass().getClassLoader();
		
		// Set the test file
		basePath = new File(classLoader.getResource("").getFile()).toPath();
		inputPath =  basePath.resolve("input" + File.separator + CMP + File.separator + RESOURCE_NAME);		
		outputPath =  basePath.resolve("output" + File.separator + CMP + File.separator + RESOURCE_NAME);
		
		testFile = new File(classLoader.getResource(RESOURCE_NAME).getFile());
		testFilePath = testFile.toPath();
			
		jsonResponseEmpty.setStatus(JsonResponseStatus.OK);
		
		listParticulier.add(entityParticulier1);
		listParticulier.add(entityParticulier2);	
		jsonResponseParticulier.setStatus(JsonResponseStatus.OK);
		jsonResponseParticulier.setPayload(listParticulier);
		
		listParticulierWithErrors.add(entityParticulier1);
		listParticulierWithErrors.add(entityParticulierError);
		jsonResponseParticulierWithErrors.setStatus(JsonResponseStatus.FATAL);
		jsonResponseParticulierWithErrors.setPayload(listParticulierWithErrors);
		
		listEntreprise.add(entityEntreprise1);
		jsonResponseEntreprise.setStatus(JsonResponseStatus.OK);
		jsonResponseEntreprise.setPayload(listEntreprise);
				
		accounts.add(_7J5SSA8);
		accounts.add(_7JU97A9);		
	}
		
	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp()
	{	
		excelSrvSpy = Mockito.spy(new ExcelService(clientBuilderMock, configDataSmartMock));
		excelSrvSpy.fileDirSrv = fileDirSrvMock;		
		excelSrvSpy.securitySrv = securitySrvMock;		
		excelSrvSpy.mailSrv = mailSrvMock;
		excelSrvSpy.messageSource = messageSourceMock;
		fileList = new ArrayList<>();
		fileList.add(inputPath.toFile());
		
		ReflectionTestUtils.setField(excelSrvSpy, "inputDir", basePath.resolve("input" + File.separator).toString());
		ReflectionTestUtils.setField(excelSrvSpy, "outputDir", basePath.resolve("output" + File.separator).toString());
		ReflectionTestUtils.setField(excelSrvSpy, "initialRow", 14);
		ReflectionTestUtils.setField(excelSrvSpy, "accountCell", 14);
		ReflectionTestUtils.setField(excelSrvSpy, "nasCell", 4);
		ReflectionTestUtils.setField(excelSrvSpy, "blankCell", Arrays.asList(new String[] {"F", "G", "H", "N", "O", "P", "Q"}));
		
		testContextLoads();
			
		createWorkbook();
		
		// Smart
		lenient().when(clientBuilderMock.build()).thenReturn(webClientMock);
		lenient().when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
		lenient().when(configDataSmartMock.getQuery()).thenReturn(queryMock);
		lenient().when(queryMock.getBaseUrl()).thenReturn(BASE_URL);		
		lenient().when(queryMock.getAccept()).thenReturn(APPLICATION_JSON);

		lenient().when(requestBodyUriSpecMock.uri(Mockito.any(Function.class))).thenReturn(requestBodySpecMock);
		lenient().when(requestBodySpecMock.header(ArgumentMatchers.any(String.class), ArgumentMatchers.any(String.class))).thenReturn(requestBodySpecMock);
		lenient().when(requestBodySpecMock.body(ArgumentMatchers.any(BodyInserter.class))).thenReturn(requestBodySpecMock);
		lenient().when(requestBodySpecMock.accept(ArgumentMatchers.any(MediaType.class))).thenReturn(requestBodySpecMock);
		lenient().when(requestBodySpecMock.retrieve()).thenReturn(responseSpecMock);
		lenient().when(responseSpecMock.onStatus(ArgumentMatchers.any(Predicate.class), ArgumentMatchers.any(Function.class))).thenReturn(responseSpecMock);
		lenient().when(responseSpecMock.bodyToMono(JsonResponse.class)).thenReturn(Mono.just(jsonResponseParticulier));
		
		// TODO to be removed
		lenient().when(securitySrvMock.getSmartJwt()).thenReturn(monoMock);
		lenient().when(securitySrvMock.getSmartJwt().block()).thenReturn(JSON_JWT);

		lenient().when(securitySrvMock.getSmartJwt()).thenReturn(monoMock);
		lenient().when(securitySrvMock.getSmartJwt().block()).thenReturn(JSON_JWT);
	}
		
	public void testContextLoads()
	{
		assertNotNull(clientBuilderMock);
		assertNotNull(webClientMock);
		assertNotNull(configDataSmartMock);
		assertNotNull(queryMock);
		assertNotNull(requestBodyUriSpecMock);
		assertNotNull(requestBodySpecMock);
		assertNotNull(responseSpecMock);
		assertNotNull(securitySrvMock);
	}
		
	@Test
	@Order(1)
	@SuppressWarnings("unchecked")
	void processExcelFileTest() throws Exception 
	{	
		// given
		try {	
			Thread.sleep(1000);
			Files.copy(testFilePath, inputPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		doNothing().when(excelSrvSpy).cleanWorkbookCell(ArgumentMatchers.any(Sheet.class));
		doNothing().when(excelSrvSpy).sendMessage(ArgumentMatchers.any(String.class));
		//doNothing().when(excelSrvSpy.fileDirSrv).moveFile(ArgumentMatchers.any(Path.class), ArgumentMatchers.any(Path.class));
		
		when(messageSourceMock.getMessage(ArgumentMatchers.any(String.class), ArgumentMatchers.any(Object[].class), ArgumentMatchers.any(Locale.class))).thenReturn("");
		when(excelSrvSpy.fileDirSrv.moveFile(ArgumentMatchers.any(Path.class), ArgumentMatchers.any(Path.class))).thenReturn(Boolean.TRUE);
						
		// when		
		excelSrvSpy.processExcelFile(fileList, CMP);		
				
		// then
		verify(excelSrvSpy).fetchAccounNumbersFromExcelFile(ArgumentMatchers.any(Workbook.class));		
		verify(excelSrvSpy).fetchNasFromSmartd(accounts);
		verify(excelSrvSpy).updateWorkbookWithNas(ArgumentMatchers.any(List.class), ArgumentMatchers.any(File.class), ArgumentMatchers.any(Sheet.class));		
		verify(excelSrvSpy).sendSuccessMessage(ArgumentMatchers.any(String.class));
		verify(excelSrvSpy).cleanWorkbookCell(ArgumentMatchers.any(Sheet.class));
		verify(fileDirSrvMock).moveFile(ArgumentMatchers.any(Path.class), ArgumentMatchers.any(Path.class));
		verify(excelSrvSpy, Mockito.times(0)).terminate(ArgumentMatchers.any(String.class), 
				ArgumentMatchers.any(Workbook.class),
				ArgumentMatchers.any(FileOutputStream.class), 
				ArgumentMatchers.any(File.class), ArgumentMatchers.any(Boolean.class) );
    }	

	 @Test
	 @Order(2) 
	 void processExcelFileNullTest() throws Exception 
	 { 
		 // given
		 when(messageSourceMock.getMessage(ArgumentMatchers.any(String.class), ArgumentMatchers.any(Object[].class), ArgumentMatchers.any(Locale.class))).thenReturn("");
		 doNothing().when(excelSrvSpy).sendMessage(ArgumentMatchers.any(String.class));
	  
		 // when 
		 excelSrvSpy.processExcelFile(null, CMP);
	  
		 // then
		 verify(excelSrvSpy).sendErrorMessage(ArgumentMatchers.any(String.class)); 
	 }
	
	 @Test
	 @Order(3) 
	 void processExcelFileEmptyTest() throws Exception 
	 { 
		 // given
		 when(messageSourceMock.getMessage(ArgumentMatchers.any(String.class), ArgumentMatchers.any(Object[].class), ArgumentMatchers.any(Locale.class))).thenReturn("");
		 doNothing().when(excelSrvSpy).sendMessage(ArgumentMatchers.any(String.class));
	  
		 // when 
		 excelSrvSpy.processExcelFile(new ArrayList<File>(), CMP);
	  
		 // then
		 verify(excelSrvSpy).sendErrorMessage(ArgumentMatchers.any(String.class));
	 }

	 @Test	  
	 @Order(4) 
	 void processNoAccountsInFileTest() throws Exception 
	 { 
		 // given		 
		 when(messageSourceMock.getMessage(ArgumentMatchers.any(String.class), ArgumentMatchers.any(Object[].class), ArgumentMatchers.any(Locale.class))).thenReturn("");
		 doNothing().when(excelSrvSpy).sendMessage(ArgumentMatchers.any(String.class));
		 doNothing().when(excelSrvSpy.fileDirSrv).deleteFile(ArgumentMatchers.any(File.class));		 
		 doReturn(new ArrayList<String>()).when(excelSrvSpy).fetchAccounNumbersFromExcelFile(ArgumentMatchers.any(Workbook.class));
	  
		 // when 
		 excelSrvSpy.processExcelFile(fileList, CMP);
	  
		 // then 
		 verify(excelSrvSpy, Mockito.times(1)).terminate(ArgumentMatchers.any(String.class), 
				 ArgumentMatchers.any(Workbook.class), 
				 ArgumentMatchers.any(FileOutputStream.class),
				 ArgumentMatchers.any(File.class), 
				 ArgumentMatchers.any(Boolean.class) );
	  
		 verify(excelSrvSpy).sendErrorMessage(ArgumentMatchers.any(String.class));
		 verify(excelSrvSpy.fileDirSrv).deleteFile(ArgumentMatchers.any(File.class));
	 }
	 
	 @Test
	 @SuppressWarnings("unchecked")
	 @Order(5) void fetchNasFromSmartThrowExceptionTest() throws Exception 
	 { 
		 // given		 
		 when(messageSourceMock.getMessage(ArgumentMatchers.any(String.class), ArgumentMatchers.any(Object[].class), ArgumentMatchers.any(Locale.class))).thenReturn("");
		 doNothing().when(excelSrvSpy).sendMessage(ArgumentMatchers.any(String.class));
		 doNothing().when(excelSrvSpy.fileDirSrv).deleteFile(ArgumentMatchers.any(File.class));		 
		 doThrow(new SmartdException()).when(excelSrvSpy).fetchNasFromSmartd(ArgumentMatchers.any(List.class));
		 
		 // when 
		 excelSrvSpy.processExcelFile(fileList, CMP);
		 		 
		 // then 
		 verify(excelSrvSpy, Mockito.times(1)).terminate(ArgumentMatchers.any(String.class),
				 ArgumentMatchers.isNull(), 
				 ArgumentMatchers.isNull(),
				 ArgumentMatchers.any(File.class), 
				 ArgumentMatchers.any(Boolean.class) ); 
	 }
	 
	 @Test 
	 @Order(6) 
	 @SuppressWarnings("unchecked")
	 void isSmartdReturnEntitiesNotValidTest() throws Exception 
	 { 
		 // given
		 // After the preceding test the excel file is not write back and therefore is empty
		 try {				 
			 // Sleep to allow other process to close the file
			 Thread.sleep(1000);
			 Files.copy(testFilePath, inputPath, StandardCopyOption.REPLACE_EXISTING);
		 } catch (Exception e) {
			 e.printStackTrace();
		 }
		 
		 when(messageSourceMock.getMessage(ArgumentMatchers.any(String.class), ArgumentMatchers.any(Object[].class), ArgumentMatchers.any(Locale.class))).thenReturn("");
		 doNothing().when(excelSrvSpy).sendMessage(ArgumentMatchers.any(String.class));
		 doNothing().when(excelSrvSpy.fileDirSrv).deleteFile(ArgumentMatchers.any(File.class));
		 doReturn(Boolean.FALSE).when(excelSrvSpy).isSmartdReturnEntitiesValid(ArgumentMatchers.any(List.class));
			
		 // when
		 excelSrvSpy.processExcelFile(fileList, CMP);		
							
		 // then
		 verify(excelSrvSpy, Mockito.times(1)).terminate(ArgumentMatchers.any(String.class), 
				 ArgumentMatchers.any(Workbook.class), 
				 ArgumentMatchers.any(FileOutputStream.class),
				 ArgumentMatchers.any(File.class), 
				 ArgumentMatchers.any(Boolean.class) );	
	 }

	@Test
	@Order(7)
    void fetchAccounNumbersFromExcelFileTest() throws Exception 
	{	
		// given
		doReturn(new ArrayList<String>()).when(excelSrvSpy).fetchAccounNumbersFromExcelFile(ArgumentMatchers.any(Workbook.class));	
		
		// when
		Workbook workbook = WorkbookFactory.create(true);
		List<String> list = excelSrvSpy.fetchAccounNumbersFromExcelFile(workbook);
		
		assertEquals(0, list.size());		
    }	

	@Test
	@Order(8)
    void fetchAccounNumbersFromEmptyExcelFileTest() throws Exception 
	{	
		// given
		doReturn(accounts).when(excelSrvSpy).fetchAccounNumbersFromExcelFile(ArgumentMatchers.any(Workbook.class));	
		
		// when
		Workbook workbook = WorkbookFactory.create(true);
		List<String> list = excelSrvSpy.fetchAccounNumbersFromExcelFile(workbook);
		
		assertEquals(2, list.size());		
    }
		
	@Test
	@Order(10)
	@SuppressWarnings("unchecked")
	void fetchNasFromSmartEmptyTest() throws Exception 
	{			
		// given	
		when(messageSourceMock.getMessage(ArgumentMatchers.any(String.class), ArgumentMatchers.any(Object[].class), ArgumentMatchers.any(Locale.class))).thenReturn("");
		doNothing().when(excelSrvSpy).sendMessage(ArgumentMatchers.any(String.class));
		doNothing().when(excelSrvSpy.fileDirSrv).deleteFile(ArgumentMatchers.any(File.class));
		doReturn(Mono.just(jsonResponseEmpty)).when(excelSrvSpy).fetchNasFromSmartd(ArgumentMatchers.any(List.class));	
		
		// when
		excelSrvSpy.processExcelFile(fileList, CMP);		
				
		// then
		verify(excelSrvSpy, Mockito.times(1)).terminate(ArgumentMatchers.any(String.class), 
				 ArgumentMatchers.any(Workbook.class), 
				 ArgumentMatchers.any(FileOutputStream.class),
				 ArgumentMatchers.any(File.class), 
				 ArgumentMatchers.any(Boolean.class) );	
    }

	@Test
	@Order(11)
    void isSmartdReturnEntitiesValidTest() throws Exception 
	{			
		// when
		boolean valid = excelSrvSpy.isSmartdReturnEntitiesValid(listParticulier);
		boolean valid1 = excelSrvSpy.isSmartdReturnEntitiesValid(listEntreprise);
		boolean valid2 = excelSrvSpy.isSmartdReturnEntitiesValid(listParticulierWithErrors);
		
		// then
		assertTrue(valid);
		assertTrue(valid1);
		assertFalse(valid2);
    }	

	@Test
	@Order(12)
    void updateWorkbookWithNasParticulierTest() throws Exception 
	{	
		// When		
		Sheet sheet =  workbook.getSheetAt(0);
		excelSrvSpy.updateWorkbookWithNas(listParticulier, testFile, sheet);
		
		// then
		assertEquals("123 456 789", sheet.getRow(INITIAL_ROW).getCell(NAS_CELL).getStringCellValue());
		assertEquals("123 456 123", sheet.getRow(INITIAL_ROW + 1).getCell(NAS_CELL).getStringCellValue());
    }	

	@Test
	@Order(13)
    void updateWorkbookWithNasEntrepriseTest() throws Exception 
	{	
		// when				
		Sheet sheet =  workbook.getSheetAt(0);
		excelSrvSpy.updateWorkbookWithNas(listEntreprise, testFile, sheet);
		
		// then
		assertEquals("", sheet.getRow(INITIAL_ROW).getCell(NAS_CELL).getStringCellValue());
    }	
	
	@Test
	@Order(14)
    void cleanWorkbookCellTest() throws Exception 
	{	
		// When		
		Sheet sheet =  workbook.getSheetAt(0);
		excelSrvSpy.cleanWorkbookCell(sheet);
		
		for (int i = INITIAL_ROW - 1; ; i++) 
		{
			Row row = sheet.getRow(i);
			String cellValue = row.getCell(ACCOUNT_CELL).getStringCellValue();
			
			// No more value to read in the excel sheet
			if (!StringUtils.hasText(cellValue))
				break;
			
			for(String cell : BLANK_CELL)
			{
				CellReference cr = new CellReference(cell + row.getRowNum());				
				assertEquals("", row.getCell(cr.getCol()).getStringCellValue());							
			}
		}
    }	

	@Test
	@Order(15)
    void terminateTest() throws Exception 
	{	
		// given
		doNothing().when(excelSrvSpy).sendMessage(ArgumentMatchers.any(String.class));
		doNothing().when(excelSrvSpy.fileDirSrv).deleteFile(ArgumentMatchers.any(File.class));
		
		boolean deleteFileOnly = false;
		String errorMessage = "error message";
		
		// when 
		excelSrvSpy.terminate(errorMessage, workbook, null, testFile, deleteFileOnly);
		
		// then
		verify(excelSrvSpy).closeStreamDeleteFile(workbook, null, testFile, deleteFileOnly);
		verify(excelSrvSpy).sendErrorMessage(errorMessage);
    }		

	@Test
	@Order(16)
    void terminateTest2() throws Exception 
	{	
		// given
		doNothing().when(excelSrvSpy).sendMessage(ArgumentMatchers.any(String.class));
		
		boolean deleteFileOnly = false;
		String errorMessage = "error message";
		
		// when 
		excelSrvSpy.terminate(errorMessage, workbook, null, testFile, deleteFileOnly);
		
		// then
		verify(excelSrvSpy, Mockito.times(0)).closeStreamDeleteFile(workbook, null, testFile, true);
		verify(excelSrvSpy, Mockito.times(0)).sendErrorMessage("");
    }
	
	@Test
	@Order(17)
    void closeStreamDeleteFileTest() throws Exception 
	{	
		// given	
		boolean deleteFileOnly = false;
		
		// when 
		excelSrvSpy.closeStreamDeleteFile(workbookMock, null, testFile, deleteFileOnly);
		
		// then
		verify(workbookMock, Mockito.times(0)).write(null);
		verify(workbookMock, Mockito.times(1)).close();
    }		
	
	@Test
	@Order(18)
    void sendSuccessMessageTest() throws Exception 
	{
		// given
		doNothing().when(excelSrvSpy).sendMessage(ArgumentMatchers.any(String.class));
		
		// when
		excelSrvSpy.sendSuccessMessage("allo");
		
		// then
		verify(excelSrvSpy).sendMessage(ArgumentMatchers.any(String.class));
    }		
	
	@Test
	@Order(19)
    void sendErrorMessageTest() throws Exception 
	{	
		// given
		doNothing().when(excelSrvSpy).sendMessage(ArgumentMatchers.any(String.class));
		
		// when
		excelSrvSpy.sendErrorMessage("allo");
		
		// then
		verify(excelSrvSpy).sendMessage(ArgumentMatchers.any(String.class));
    }
	
	@Test
	@Order(20)
    void sendSuccessMessageEmptyTest() throws Exception 
	{
		// given
		doNothing().when(excelSrvSpy).sendMessage(ArgumentMatchers.any(String.class));
		
		// when
		excelSrvSpy.sendSuccessMessage("");
		
		// then
		verify(excelSrvSpy, Mockito.timeout(0)).sendMessage(ArgumentMatchers.any(String.class));
    }		
	
	@Test
	@Order(21)
    void sendErrorMessageEmptyTest() throws Exception 
	{	
		// given
		doNothing().when(excelSrvSpy).sendMessage(ArgumentMatchers.any(String.class));
		
		// when
		excelSrvSpy.sendErrorMessage("");
		
		// then
		verify(excelSrvSpy, Mockito.timeout(0)).sendMessage(ArgumentMatchers.any(String.class));
    }
	
	@Test
	@Order(22)
    void getTokenTest() throws Exception 
	{
		// Given
		
		// when		
		String token = excelSrvSpy.getToken();		
				
		// then				
		assertNotNull(token);
		assertFalse(token.startsWith(ACCESS_TOKEN));		
	}	
	
	@Test
	@Order(23)
    void getTokenNullTest() throws Exception 
	{
		// Given
		lenient().when(securitySrvMock.getSmartJwt().block()).thenReturn(null);
		
		// when		
		SmartdException ex = assertThrows(SmartdException.class, () -> excelSrvSpy.getToken());		
				
		// then						
		assertEquals(TOKEN_EXCEPTION_MSG, ex.getMessage());		
	}		
	
	private void createWorkbook()
	{
		try
		{
			workbook = WorkbookFactory.create(true);
			Sheet sheet = workbook.createSheet();
			for (int i = 0; i < 20; i++) {
				Row row = sheet.createRow(i);
				for (int j = 0; j < 20; j++)
					row.createCell(j);
			}
			
			Row row = sheet.getRow(INITIAL_ROW);
			Cell cell = row.getCell(ACCOUNT_CELL);
			cell.setCellValue(_7J5SSA8);
			row = sheet.getRow(INITIAL_ROW + 1);
			cell = row.getCell(ACCOUNT_CELL);
			cell.setCellValue(_7JU97A9);
		} catch(IOException ioe) {
			System.out.println("Error creating Workbook");
		}		
	}
	
}
