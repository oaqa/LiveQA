package edu.cmu.lti.oaqa.cache;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;

/**
 * Cache Java binary object
 * @author Di Wang
 * @param <T> Java POJO class
 */
public class MongoPojoCache<T> implements KeyObjectCache<T> {
    private final String dbname = "liveqa";

    String mongodb_host = "localhost";

    int mongodb_port = 27017;

    private GridFS gridfs;

    public MongoPojoCache(String bucket) {
        MongoClient mongo;
        try {
            mongo = new MongoClient(mongodb_host, mongodb_port);
            DB db = mongo.getDB(dbname);
            db.authenticate("username", "password".toCharArray()); //FIXME: load from config file
            gridfs = new GridFS(db, bucket);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public MongoPojoCache(Class<?> aClass) {
        this(aClass.getSimpleName());
    }

    public MongoPojoCache(Object obj) {
        this(obj.getClass());
    }

    @Override
    public void put(String keyText, T obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream objectOutput = new ObjectOutputStream(baos);
        objectOutput.writeObject(obj);
        objectOutput.close();
        byte[] binaryObject = baos.toByteArray();

        GridFSInputFile objFile = gridfs.createFile(binaryObject);
        objFile.setFilename(DigestUtils.sha256Hex(keyText));
        //it will not update content of existing file
        objFile.save();
    }

    @Override
    public T get(String keyText) throws IOException, ClassNotFoundException {
        GridFSDBFile objFile = gridfs.findOne(DigestUtils.sha256Hex(keyText));
        if (objFile == null) {
            return null;
        }
        ObjectInputStream objectInput = new ObjectInputStream(objFile.getInputStream());
        T annotation = (T) objectInput.readObject();
        objectInput.close();
        return annotation;
    }

    @Override
    public void del(String keyText) throws IOException, ClassNotFoundException {
        gridfs.remove(DigestUtils.sha256Hex(keyText));
    }

}
