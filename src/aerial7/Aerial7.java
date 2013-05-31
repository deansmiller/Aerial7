package aerial7;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
	private DBCollection items;
	private AmazonS3 s3;
	private final String BUCKET = "dam-content-bucket";
	private final String CONTENT_DIR = "dam-content/";
       
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
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	}
	
	private void updateItem(String itemId, Color color){
		ObjectId id = new ObjectId(itemId);
		BasicDBObject searchQuery = new BasicDBObject("_id", id);
		BasicDBObject colourQuery = new BasicDBObject();
		colourQuery.append("$set", new BasicDBObject("fileData", new BasicDBObject("colour", color.getRed() + "," + color.getGreen() + "," + color.getBlue())));
		WriteResult result = items.update(searchQuery, colourQuery);
		System.out.println(result);
	}
	
	private Color processImage(BufferedImage img){
		int width = img.getWidth();
		int height = img.getHeight();
		int red = 0;
		int green = 0;
		int blue = 0;
		int count = 0;
		Color color;
		for(int i = 0; i < width; i++){
			for(int j = 0; j < height; j++){
				color = new Color(img.getRGB(i, j));
				red += color.getRed();
				green += color.getGreen();
				blue += color.getBlue();
				count++;
			}
		}
		return new Color(red / count, green / count, blue / count);
	}
	
	
	private BufferedImage convertToImage(S3Object item){
		byte[] _bytes = null;
		try {
			S3ObjectInputStream stream = item.getObjectContent();
			int i = 0;
			ArrayList<Integer> bytes = new ArrayList<Integer>();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			while((i = stream.read()) != -1){
				baos.write(i);
			}

			_bytes = baos.toByteArray();
			stream.read(_bytes);
			stream.close();
			return ImageIO.read(new ByteArrayInputStream(_bytes));	
		} catch(IOException e){
			System.out.println(e);
		}
		
		return null;
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String cmd = request.getParameter("cmd");
		String itemId = request.getParameter("item");
		String filename = request.getParameter("filename");
		
		if(cmd.equals("PROCESS")){
			S3Object item = s3.getObject(BUCKET, CONTENT_DIR + itemId + "/" + filename);
			BufferedImage convertedImg = convertToImage(item);
			Color colour = processImage(convertedImg);
			updateItem(itemId, colour);
		}
	}

}
