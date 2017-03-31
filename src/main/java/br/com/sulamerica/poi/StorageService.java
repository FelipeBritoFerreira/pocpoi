package br.com.sulamerica.poi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.Objects;
import com.google.api.services.storage.model.StorageObject;





public class StorageService {

	
	private Log log = LogFactory.getLog(StorageService.class);
	
	private Storage storageService;
	private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final String APPLICATION_NAME = "pocpoi";
	private static final String BUCKET_NAME = "pocpoi-144317.appspot.com";
	private static final String SERVICE_ACCOUNT_EMAIL = "pocpoi-bucket@pocpoi-144317.iam.gserviceaccount.com";
	private static final String NOME_PASTA = "planilhas/";
	private PrivateKey key;

	
	
	/**
	 * Construtor que ja busca a chave de acesso
	 */
	public StorageService() {
		try {
			key = loadKeyFromPkcs12("notasecret".toCharArray());
		} catch (Exception ex) {
			throw new RuntimeException("Erro - ", ex);
		}
	}
	
	
	
	
	/**
	 * Faz o download da planilha gerada
	 * @return
	 * @throws Exception
	 */
	public String download(String nomeArquivo) throws Exception {
		
		List<StorageObject> objetos  = listarObjetosStorage();
		
		for ( StorageObject obj : objetos  ) {
			
			//Pega apenas os objetos da pasta "planilhas"
			if (  obj.getName().equals(NOME_PASTA) || !obj.getName().startsWith(NOME_PASTA)  ) {
				continue;
			}
			
			//Arranca fora do nome o primeiro "/" e a extensão ".xlsx"
			String nomeObjeto = obj.getName().substring( obj.getName().lastIndexOf("/") + 1, obj.getName().length() - 5 ); 
			
			if (  nomeObjeto.equals(nomeArquivo) ) {
				return getSigningURL("GET", obj.getName());
			}
			
		}
		
		return null;
	}	
	
	
	
	
	
	
	
	/**
	 * Upload de nova planilha
	 * Devolve o nome do arquivo
	 * @throws Exception 
	 */
	public String upload() throws Exception {
		
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmsssss");
		
		Date date = new Date();
		
		String nomeArquivo = sdf.format(date);
		
		
		PlanilhaService planilhaService = new PlanilhaService();
		
		byte[] bytes =  planilhaService.gerarPlanilha();
		
		InputStream inputStream = new ByteArrayInputStream(bytes);
		
		InputStreamContent mediaContent = new InputStreamContent("application/octet-stream", inputStream);
		// Knowing the stream length allows server-side optimization, and client-side progress
		// reporting with a MediaHttpUploaderProgressListener.
		mediaContent.setLength(bytes.length);

		StorageObject objectMetadata = null;

		Storage.Objects.Insert insertObject = getStorageService().objects().insert(BUCKET_NAME, objectMetadata, mediaContent);

  	    insertObject.setName(NOME_PASTA + nomeArquivo + ".xlsx");


		// For small files, you may wish to call setDirectUploadEnabled(true), to
		// reduce the number of HTTP requests made to the server.
		if (mediaContent.getLength() > 0 && mediaContent.getLength() <= 2 * 1000 * 1000 /* 2MB */) {
		  insertObject.getMediaHttpUploader().setDirectUploadEnabled(true);
		}

		insertObject.execute();		
		
		
		return nomeArquivo;
		
		
	}
	
	
	
	
	
	
	
	
	

	
	/**
	 * Lista todos os objetos do storage
	 * @return
	 * @throws Exception
	 */
	private List<StorageObject> listarObjetosStorage() throws Exception {
		
		List<StorageObject> objetos = new ArrayList<>();
		
		Storage.Objects.List listObjects = getStorageService().objects().list(BUCKET_NAME);
		
		Objects objects;
		
		do {
			  objects = listObjects.execute();

			  for (StorageObject object : objects.getItems()) {
				  objetos.add(object);
			  }
			  
			  listObjects.setPageToken(objects.getNextPageToken());
			  
			} 
		
		while (null != objects.getNextPageToken());
		
		
		return objetos;
		

	}
	
	
	
	
	
	/************************
	 * 
	 * 		AUTENTICACAO
	 * 
	 * **********************
	 */
	
	
	/**
	 * Servico de autenticacao para acesso ao bucket. Funciona em qualquer ambiente, inclusive LOCAL
	 * 
	 * @return
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	private Storage getStorageService() throws IOException, GeneralSecurityException {
		
		log.info("[STORAGE] - Autenticacao");

		if (null == storageService) {
			
			PrivateKey serviceAccountPrivateKey = SecurityUtils.loadPrivateKeyFromKeyStore(SecurityUtils.getPkcs12KeyStore(), this.getClass().getClassLoader().getResourceAsStream("pocpoi-05878e7196e6.p12"), "notasecret", "privatekey", "notasecret");

			HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			
			GoogleCredential credential = new GoogleCredential.Builder()
					.setTransport(httpTransport)
					.setJsonFactory(JSON_FACTORY)
					.setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
					.setServiceAccountScopes(Collections.singleton(StorageScopes.DEVSTORAGE_FULL_CONTROL))
					.setServiceAccountPrivateKey(serviceAccountPrivateKey).build()
					
					;
			
				if (credential.createScopedRequired()) {
					credential = credential.createScoped(StorageScopes.all());
				}
				

			storageService = new Storage.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
		}
		
		return storageService;
	}

	
	/**
	 * Assinatura digital de URL
	 * 
	 * @param verb
	 * @param objectName
	 * @return
	 * @throws Exception
	 */
	private String getSigningURL(String verb, String objectName) throws Exception {

		String signed_url = "";
		Calendar c = Calendar.getInstance();
		c.add(Calendar.YEAR, 10000);
		final long expiration = c.getTimeInMillis();

		String url_signature = this.signString(verb + "\n\n\n" + expiration + "\n" + "/" + BUCKET_NAME + "/" + objectName);
		signed_url = "https://storage.googleapis.com/" + BUCKET_NAME + "/" + objectName + "?GoogleAccessId="
				+ SERVICE_ACCOUNT_EMAIL + "&Expires=" + expiration + "&Signature="
				+ URLEncoder.encode(url_signature, "UTF-8");

		return signed_url;
	}
	
	
	/**
	 * URL assinada
	 * 
	 * @param stringToSign
	 * @return
	 * @throws Exception
	 */
	private String signString(String stringToSign) throws Exception {
		
		if (key == null) {
			throw new Exception("Private Key not initalized");
		}
		Signature signer = Signature.getInstance("SHA256withRSA");
		signer.initSign(key);
		signer.update(stringToSign.getBytes("UTF-8"));
		byte[] rawSignature = signer.sign();
		return new String(Base64.encodeBase64(rawSignature, false), "UTF-8");
	}	
	
	
	/**
	 * Busca a chave de acesso P12
	 * 
	 * @param password
	 * @return
	 * @throws Exception
	 */
	private PrivateKey loadKeyFromPkcs12(char[] password) throws Exception {
		
		InputStream fis = this.getClass().getClassLoader().getResourceAsStream("pocpoi-05878e7196e6.p12");
		log.info(String.format("Carregando p12 %s", fis));
		KeyStore ks = KeyStore.getInstance("PKCS12");
		ks.load(fis, password);
		return (PrivateKey) ks.getKey("privatekey", password);
	}	
	
	
	

}
