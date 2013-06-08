package aerial7;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Calendar;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteResult;


/**
 * Servlet implementation class Aerial7
 */
public class Aerial7 extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private MongoClient mongoClient;
	private DBCollection items, jobs;
	private AmazonS3 s3;
	private final String BUCKET = "dam-content-bucket";
	private final String CONTENT_DIR = "dam-content/";
	private Calendar calendar;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Aerial7() {
        super();        
        try {
            AWSCredentials credentials = new PropertiesCredentials(getClass().getClassLoader().getResourceAsStream("AwsCredentials.properties"));
            s3  = new AmazonS3Client(credentials);
    		s3.setRegion(Region.getRegion(Regions.EU_WEST_1));
			mongoClient = new MongoClient(new MongoClientURI("mongodb://nodejitsu:7b9b1cc97d6ad36ad901b426d425e436@linus.mongohq.com:10063/nodejitsudb6132056633"));
			DB db = mongoClient.getDB("nodejitsudb6132056633");
			items = db.getCollection("assetitems");
			jobs = db.getCollection("aerial7jobs");
			calendar = Calendar.getInstance();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().write("Testing..");
	}
	
	private WriteResult updateItem(String itemId, float[] colour){
		ObjectId id = new ObjectId(itemId);
		BasicDBObject searchQuery = new BasicDBObject("_id", id);
		BasicDBObject colourQuery = new BasicDBObject();
		colourQuery.append("$set", new BasicDBObject("fileData", new BasicDBObject("colour", colour[0] + "," + colour[1] + "," + colour[2])));
		WriteResult result = items.update(searchQuery, colourQuery);
		System.out.println(id + " -> " + result);
		return result;
	}
	
	private void createJobRecord(String itemId, String filename){
		BasicDBObject job = new BasicDBObject();
		job.put("itemId", itemId);
		job.put("filename", filename);
		job.put("date", calendar.getTime());
		jobs.insert(job);
	}
	
	private float[] processImage(BufferedImage img){
		int width = img.getWidth();
		int height = img.getHeight();
		int red = 0; int green = 0; int blue = 0; int count = 0;
		Color color;
		//should scale the image first
		
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				color = new Color(img.getRGB(i, j));
				red += color.getRed();
				green += color.getGreen();
				blue += color.getBlue();
				count++;
			}
		}
		
		float[] hsbColours = new float[3];
		color = new Color(red / count, green / count, blue / count);
		RGBtoHSV(red / count, green / count, blue / count, hsbColours);
		
		System.out.println("HSB -> " + hsbColours[0] + " " + hsbColours[1] + " " + hsbColours[2]);
		System.out.println("RGB -> " + color);
		
		return hsbColours;
	}
	
	//http://www.f4.fhtw-berlin.de/~barthel/ImageJ/ColorInspector/HTMLHelp/farbraumJava.htm
	private void RGBtoHSV(float r, float g, float b, float hsl[]) {
		
		float var_R = ( r / 255f );                    
		float var_G = ( g / 255f );
		float var_B = ( b / 255f );
		
		float var_Min;    //Min. value of RGB
		float var_Max;    //Max. value of RGB
		float del_Max;    //Delta RGB value
						 
		if (var_R > var_G) 
			{ var_Min = var_G; var_Max = var_R; }
		else 
			{ var_Min = var_R; var_Max = var_G; }

		if (var_B > var_Max) var_Max = var_B;
		if (var_B < var_Min) var_Min = var_B;

		del_Max = var_Max - var_Min; 
								 
		float H = 0, S, L;
		L = ( var_Max + var_Min ) / 2f;
	
		if ( del_Max == 0 ) { H = 0; S = 0; } // gray
		else {                                //Chroma
			if ( L < 0.5 ) 
				S = del_Max / ( var_Max + var_Min );
			else           
				S = del_Max / ( 2 - var_Max - var_Min );
	
			float del_R = ( ( ( var_Max - var_R ) / 6f ) + ( del_Max / 2f ) ) / del_Max;
			float del_G = ( ( ( var_Max - var_G ) / 6f ) + ( del_Max / 2f ) ) / del_Max;
			float del_B = ( ( ( var_Max - var_B ) / 6f ) + ( del_Max / 2f ) ) / del_Max;
	
			if ( var_R == var_Max ) 
				H = del_B - del_G;
			else if ( var_G == var_Max ) 
				H = ( 1 / 3f ) + del_R - del_B;
			else if ( var_B == var_Max ) 
				H = ( 2 / 3f ) + del_G - del_R;
			if ( H < 0 ) H += 1;
			if ( H > 1 ) H -= 1;
		}
		hsl[0] = (int)(360*H);
		hsl[1] = (int)(S*100);
		hsl[2] = (int)(L*100);
		
	}
	
	
	private BufferedImage convertToImage(S3Object item){
		try {
			S3ObjectInputStream stream = item.getObjectContent();
			byte[] bytes = IOUtils.toByteArray(stream);
			return ImageIO.read(new ByteArrayInputStream(bytes));	
		} catch(IOException e){
			System.out.println(e);
			return null;
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String cmd = request.getParameter("cmd");
		String itemId = request.getParameter("item");
		String filename = request.getParameter("filename");
		
		if(cmd.equals("PROCESS")){
			S3Object item = getItem(itemId, filename);
			BufferedImage convertedImg = convertToImage(item);
			float[] colour = processImage(convertedImg);
			updateItem(itemId, colour);
			createJobRecord(itemId, filename);
		}
	}
	
	private S3Object getItem(String itemId, String filename) {
		return s3.getObject(BUCKET, CONTENT_DIR + itemId + "/" + filename);
	}

}
