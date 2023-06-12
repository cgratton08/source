package com.desjardins.marchecapitaux.niad.security;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.desjardins.marchecapitaux.niad.security.dto.SmartdEntity;
import com.desjardins.marchecapitaux.niad.security.dto.security.JwtBody;
import com.desjardins.marchecapitaux.niad.security.utils.TypeClient;

public class BaseTest 
{
	public static final String CMP = "cmp";
	public static final String CMP1 = "cmp1";	
	public static final String BASE_URL = "http://127.0.0.1";
	public static final String APPLICATION_JSON = "application/json";
	
	// Jwt
	public static final String ACCESS_TOKEN = "access_token";
	public static final String TOKEN_EXCEPTION_MSG = "Error retrieving JWT from PISE token is null or empty";
	
	// Mail
	public static final String MESSAGE_STR = "message";
	public static final String MESSAGE_SUBSTITUTION_STR = "@msg";
	
	public static final String BODY_IDENTIFICATION_APPELANTE_KEY =  "identifiantApplicationAppelante";
	public static final String BODY_IDENTIFICATION_APPELANTE_VAL = "GTD-60185-DESJARDINS-CI";
	public static final String BODY_ADRESSE_COURRIEL_EXPEDITEUR_KEY = "adresseCourrielExpediteur";
	public static final String BODY_ADRESSE_COURRIEL_EXPEDITEUR_VAL = "expediteur@test.test";
	public static final String BODY_NOM_EXPEDITEUR_KEY = "nomExpediteur";	
	public static final String BODY_OBJET_SUJET_KEY = "objetSujet";
	public static final String BODY_OBJET_SUJET_VAL = "Communiqué de Niad-Security";
	public static final String BODY_ADRESSE_COURRIEL_KEY = "adresseCourriel";
	public static final String BODY_ADRESSE_COURRIEL_VAL = "destinataire@test.test";
	public static final String BODY_LIST_COPIE_CARBONE_KEY = "listCopieCarbone";
	public static List<String> BODY_LIST_COPIE_CARBONE_VAL = Arrays.asList(new String[] {"allo1@test.test", "allo2@test.test", "allo3@test.test"});
	public static final String BODY_TYPE_CONFIDENTIALITE_KEY = "typeConfidentialite";
	public static final String BODY_TYPE_CONFIDENTIALITE_VAL = "INTERNE";
	public static final String BODY_OBJET_CONTENU_ENRICHI_KEY = "objetContenuEnrichi";
	public static final String BODY_OBJET_CONTENU_ENRICHI_VAL = "<html><head></head><body>Communiqué de Niad-Security<br><br>@msg</body></html>";

	// Security
	public static final String BODY_EMAIL_CLIENT_ID_KEY = "emailClientId";
	public static final String BODY_EMAIL_CLIENT_ID_VAL = "EMAIL_CLIENT_ID";
	public static final String BODY_EMAIL_CLIENT_SECRET_KEY = "emailClientSecret";
	public static final String BODY_EMAIL_CLIENT_SECRET_VAL = "EMAIL_CLIENT_SECRET";
	public static final String BODY_EMAIL_USERNAME_KEY = "emailServiceAccountUsername";
	public static final String BODY_EMAIL_USERNAME_VAL = "EMAIL_COMPTE_SERVICE_USERNAME";
	public static final String BODY_EMAIL_PASSWORD_KEY = "emailServiceAccountPassword";
	public static final String BODY_EMAIL_PASSWORD_VAL = "EMAIL_COMPTE_SERVICE_PASSWORD";
	public static final String BODY_EMAIL_GRANT_TYPE_KEY = "emailGrantType";
	public static final String BODY_EMAIL_GRANT_TYPE_VAL = "password";
	public static final String BODY_EMAIL_SCOPE_KEY = "emailScope";
	public static final String BODY_EMAIL_SCOPE_VAL = "communication.expedition.courriels.envoyer";

	public static final String BODY_SMART_CLIENT_ID_KEY = "smartClientId";
	public static final String BODY_SMART_CLIENT_ID_VAL = "EMAIL_CLIENT_ID";
	public static final String BODY_SMART_CLIENT_SECRET_KEY = "smartClientSecret";
	public static final String BODY_SMART_CLIENT_SECRET_VAL = "EMAIL_CLIENT_SECRET";
	public static final String BODY_SMART_USERNAME_KEY = "smartServiceAccountUsername";
	public static final String BODY_SMART_USERNAME_VAL = "EMAIL_COMPTE_SERVICE_USERNAME";
	public static final String BODY_SMART_PASSWORD_KEY = "smartServiceAccountPassword";
	public static final String BODY_SMART_PASSWORD_VAL = "EMAIL_COMPTE_SERVICE_PASSWORD";
	public static final String BODY_SMART_GRANT_TYPE_KEY = "smartGrantType";
	public static final String BODY_SMART_GRANT_TYPE_VAL = "password";
	public static final String BODY_SMART_SCOPE_KEY = "smartScope";
	public static final String BODY_SMART_SCOPE_VAL = "niad.retrieve.accounts.info";
	
	// JWT
	public static final String JWTBODY_CLIENT_ID_KEY = "client_id";
	public static final String JWTBODY_CLIENT_SECRET_KEY = "client_secret";
	public static final String JWTBODY_USERNAME_KEY = "username";
	public static final String JWTBODY_PASSWORD_KEY = "password";
	public static final String JWTBODY_GRANT_TYPE_KEY = "grant_type";
	public static final String JWTBODY_SCOPE_KEY = "scope";
	
	public ClassLoader classLoader;
	public File testFile;
	public Path testFilePath;

	// Test file path
	public Path basePath;
	public Path inputPath;
	public Path outputPath;
	
	// File resource for test
    public static final int INITIAL_ROW = 14;
    public static final int ACCOUNT_CELL = 14;
    public static final int NAS_CELL = 4;
    public static final List<String> BLANK_CELL = Arrays.asList(new String[] {"F", "G", "H", "N", "O", "P", "Q"});

    public static final String JSON_JWT =  "{\"access_token\": \"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6InQxb1Uz\"}";
	
	public static final String RESOURCE_NAME = "PurchasersList.xls";	
	public List<File> fileList = new ArrayList<>();
	
	public List<String> accounts = new ArrayList<>();
	public final static String _7J5SSA8 = "7J5SSA8";
	public final static String _7JU97A9 = "7JU97A9";
	
	public SmartdEntity entityParticulier1 = SmartdEntity.builder()
			.numeroClient("7J5SSA8")
			.numeroCompte("7J5SSA8")
			.numeroNAS("123 456 789")
			.typeClient(TypeClient.PARTICULIER)
			.build();
			
	public SmartdEntity entityParticulier2 = SmartdEntity.builder()
			.numeroClient("7JU97A9")
			.numeroCompte("7JU97A9")
			.numeroNAS("123 456 123")
			.typeClient(TypeClient.PARTICULIER)
			.build();				 	
	
	public SmartdEntity entityParticulierError = SmartdEntity.builder()
			.numeroClient("7JU97A9")
			.numeroCompte("7JU97A9")
			.numeroNAS("")
			.typeClient(TypeClient.PARTICULIER)
			.build();
	
	public SmartdEntity entityEntreprise1 = SmartdEntity.builder()
			.numeroClient("123abc")
			.numeroCompte("123abc")
			.numeroNAS("")
			.typeClient(TypeClient.ENTREPRISE)
			.build();
	
	public JwtBody jwtEmailBody = JwtBody.builder()
			.client_id(BODY_EMAIL_CLIENT_ID_VAL)
			.client_secret(BODY_EMAIL_CLIENT_SECRET_VAL)
			.username(BODY_EMAIL_USERNAME_VAL)
			.password(BODY_EMAIL_PASSWORD_VAL)
			.scope(BODY_EMAIL_SCOPE_VAL)
			.grant_type(BODY_EMAIL_GRANT_TYPE_VAL)
			.build();
	
	public JwtBody jwtSmartBody = JwtBody.builder()
			.client_id(BODY_SMART_CLIENT_ID_VAL)
			.client_secret(BODY_SMART_CLIENT_SECRET_VAL)
			.username(BODY_SMART_USERNAME_VAL)
			.password(BODY_SMART_PASSWORD_VAL)
			.scope(BODY_SMART_SCOPE_VAL)
			.grant_type(BODY_SMART_GRANT_TYPE_VAL)
			.build();	
}
