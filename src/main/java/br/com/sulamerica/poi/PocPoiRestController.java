package br.com.sulamerica.poi;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Spring MVC Controller
 * <br>	
 * Controla as chamadas das telas
 * @author Felipe Brito
 *
 */
@RestController
@RequestMapping("/rest")
public class PocPoiRestController {
	
	
	/**
	 * Testes com o storage (bucket)
	 * @return
	 * @throws Exception 
	 */
	@RequestMapping(value="/planilha")
	public String gerarPlanilha() throws Exception {
		
		StorageService storageService = new StorageService();
		
		String nomeArquivo = storageService.upload();
		
		String url = storageService.download(nomeArquivo);
		
		return url;

	}
	
	

}





