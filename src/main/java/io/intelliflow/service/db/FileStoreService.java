package io.intelliflow.service.db;

import io.intelliflow.dto.model.db.FileStoreDto;
import io.intelliflow.model.db.FileStore;
import io.intelliflow.repository.FileStoreRepository;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Parameters;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FileStoreService {

    @Inject
    FileStoreRepository fileStoreRepository;

    public FileStoreDto saveToRepository(Path file, FileStoreDto fileStoreDto) {
        UUID fileId = null;

        //time to live for file, default 24hr
        int ttlForFile = 86400;

        //size for each chunk in db partition, default 2MB
        int chunkSize = 1024*1024*2;

        //loop variable, tracking order of partitions
        int orderKey =0;

        // File Name
        String fileName = fileStoreDto.getName() + "." + fileStoreDto.getFormat();
        FileStore fileStoreObj = null;
        try {
            //creating channel of whole file
            FileChannel channel = FileChannel.open(file);

            //creating a buffer of 2MB , for chunking
            ByteBuffer partitionBlob = ByteBuffer.allocateDirect(chunkSize);

            // Number of loops to store all partitions
            int loopKey = (int)channel.size() / chunkSize;

            // if any bytes remaining other than chunk size
            int backlogBytes = (int)channel.size() % chunkSize;

            // track the chunkSize of partitions being committed
            int position = 0;

            FileStore savedFile = null;

            fileId = UUID.randomUUID();
            while(orderKey < loopKey){
                channel.read(partitionBlob, position);
                partitionBlob.flip();
                fileStoreObj = new FileStore(fileName, orderKey, fileId, partitionBlob, fileStoreDto.getStatus(), fileStoreDto.getAppName(), fileStoreDto.getUser());
                fileStoreRepository.persist(fileStoreObj);
                partitionBlob.clear();
                orderKey++;
                position += chunkSize;
            }

            if(backlogBytes > 0){
                ByteBuffer buffRemain = ByteBuffer.allocateDirect(backlogBytes);
                channel.read(buffRemain, position);
                buffRemain.flip();
                fileStoreObj = new FileStore(fileName, orderKey, fileId, buffRemain, fileStoreDto.getStatus(), fileStoreDto.getAppName(), fileStoreDto.getUser());
                fileStoreRepository.persist(fileStoreObj);
                buffRemain.clear();
                position += backlogBytes;
            }

            fileStoreDto.setFileName(fileName);
            /*
            * savedFile shall return null if the save is success
            * else it returns the existing object after updating
            * */
            fileStoreDto.setId(fileStoreObj != null ? fileStoreObj.getId() : fileId);

        } catch (IOException e) {
            Log.error("Partition & Save Operation Failed!!");
            Log.error(e);
            e.printStackTrace();
        }
        return fileStoreDto.getId() != null ? fileStoreDto : null;
    }


    public FileStoreDto fetchFileByNameAndId(UUID fileId, String fileName) {

        FileStoreDto fileStoreDto = null;
         try {
            List<FileStore> fileStores = fileStoreRepository.getByFileNameAndId(fileId, fileName);
            FileOutputStream fout = new FileOutputStream("D:/Projects/SampleApp/newtext.txt");
            for(FileStore fileStore : fileStores){
                assert fileStore!= null;
                System.out.println("Order Key : " + fileStore.getOrderkey());
                fout.write(fileStore.getPartition().array());
                // this will loop, should avoid
                fileStoreDto = new FileStoreDto(
                        fileStore.getId(),
                        fileStore.getName(),
                        null,
                        fileStore.getAppname(),
                        fileStore.getUser(),
                        null,
                        fileStore.getStatus(),
                        null
                );
            }

            fout.close();
        } catch (IOException e) {
            Log.error("Partition Retrieve Operation Failed!!");
            e.printStackTrace();
        }
         return fileStoreDto;
    }
}
