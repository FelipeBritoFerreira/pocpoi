package br.com.sulamerica.poi;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * POC Exemplo de geracao de planilha
 * @author FBF0113
 *
 */
public class PlanilhaService {

	
	
	public byte[] gerarPlanilha() throws Exception  {
		
		
		//Inicia a criacao de uma nova planilha
		XSSFWorkbook wb = new XSSFWorkbook();
		
		//Cria uma nova planilha
		XSSFSheet planilha = wb.createSheet("Exemplo");
		
		//Cria o objeto "linha"
		XSSFRow row;
		
		//Abaixo, os valores que serao inseridos
		Map < String, Object[] > valores = new TreeMap < String, Object[] >();
		
		valores.put( "1", new Object[] {"ID", "NOME", "CPF" });
		valores.put( "2", new Object[] {"tp01", "Felipe", "123456789" });
		valores.put( "3", new Object[] {"tp02", "Borba", "321654987" });
		valores.put( "4", new Object[] {"tp03", "Barbero", "456789321" });
		valores.put( "5", new Object[] {"tp04", "Beavis", "456789159" });
		valores.put( "6", new Object[] {"tp05", "Peruano", "951465894" });
		
		
		
		//Itera sobre a lista e popula a planilha
		Set < String > keyid = valores.keySet();
		
		int rowid = 0;
		
		for (String key : keyid) {
		   
			row = planilha.createRow(rowid++);
			
			Object [] objectArr = valores.get(key);
			int cellid = 0;
			
			for (Object obj : objectArr) {
		      
			   Cell cell = row.createCell(cellid++);
		       cell.setCellValue((String)obj);
		       
		   }
		}
		
		//Gera o arquivo
		
		//FileOutputStream out = new FileOutputStream( new File("Planilha.xlsx") );
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		wb.write(out);
		
		wb.close();
		
		
		return out.toByteArray();
		
	}
	
	

	

}
