package io.intelliflow.modeller;

import javax.inject.Inject;

import io.intelliflow.centralCustomExceptionHandler.CustomException;
import io.intelliflow.dto.bindproperty.BasePropertyDto;
import io.intelliflow.dto.bindproperty.BindPropertyList;
import io.intelliflow.dto.model.BaseModelList;
import io.intelliflow.dto.model.FileModelDto;
import io.intelliflow.service.FileOperations;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.intelliflow.dto.model.BaseDataModelDTO;
import io.intelliflow.dto.repomanager.EventResponseModel;
import io.intelliflow.dto.repomanager.FileInformation;
import io.intelliflow.service.repomanager.ExtensionService;
import io.smallrye.mutiny.Uni;

import java.util.List;

public abstract class BaseModeller {

	@ConfigProperty(name = "miniapp.resource.dir")
	String resourceLocation;

	@Inject
	@RestClient
	ExtensionService extensionService;


	public Uni<EventResponseModel> createFile(BaseDataModelDTO baseDataModel,String userId) throws CustomException {
			FileInformation fileInfo = new FileInformation();

			if(baseDataModel.getFileType().equals("drl")) {
				fileInfo.setFileName(baseDataModel.getFileName() + ".drl");
				fileInfo.setFileType("dmn");
			} else if(baseDataModel.getFileType().equals("form") || baseDataModel.getFileType().equals("page")) {
				if(baseDataModel.getFileType().equals("page")) {
					fileInfo.setFileType("page");
					fileInfo.setFileName(baseDataModel.getFileName() + ".page");
				} else {
					fileInfo.setFileType("form");
					fileInfo.setFileName(baseDataModel.getFileName() + ".frm");
				}
			} else if(baseDataModel.getFileType().equals("datamodel")) {
				fileInfo.setFileName(baseDataModel.getFileName() + ".java");
				fileInfo.setFileType(baseDataModel.getFileType());
			} else if(baseDataModel.getFileType().equals("bpmn")) {
				fileInfo.setFileName(baseDataModel.getFileName() + ".bpmn");
				fileInfo.setFileType(baseDataModel.getFileType());
				if(baseDataModel.getFileContent() != null) {
					FileInformation metaFile = FileOperations.createFileInfoForMeta(baseDataModel);
					extensionService.createMetaFileInWorkspace(metaFile).subscribe()
							.with(
									item -> System.out.println("Meta Created for BPMN")
							);
				}

			} else {
				fileInfo.setFileName(baseDataModel.getFileName() + "." + baseDataModel.getFileType());
				fileInfo.setFileType(baseDataModel.getFileType());
			}
			if(baseDataModel.getFileContent() != null) {
				fileInfo.setContent(new String(baseDataModel.getFileContent()));
			}
			fileInfo.setWorkspaceName(baseDataModel.getWorkspaceName());
			fileInfo.setMiniApp(baseDataModel.getMiniAppName());
			if(baseDataModel.getComment() != null) {
				fileInfo.setComment(baseDataModel.getComment());
			} else {
				fileInfo.setComment("Created " + baseDataModel.getFileName() + " in Workspace");
			}
			fileInfo.setUserId(userId);
			return extensionService.createFileInRepo(fileInfo);
	}

	public Uni<EventResponseModel> deleteFile(BaseDataModelDTO baseDataModel,String userId) {
		FileInformation fileInfo = new FileInformation();
		fileInfo.setWorkspaceName(baseDataModel.getWorkspaceName());
		fileInfo.setMiniApp(baseDataModel.getMiniAppName());
		fileInfo.setFileName(baseDataModel.getFileName());
		fileInfo.setUserId(userId);
		if(baseDataModel.getFileType().equals("drl")) {
			fileInfo.setFileType("dmn");
		} else {
			fileInfo.setFileType(baseDataModel.getFileType());
		}
		fileInfo.setComment("Deleted file " + baseDataModel.getFileName());
		return extensionService.deleteFileInRepo(fileInfo);
	}

	public Uni<EventResponseModel> updateFile(BaseDataModelDTO baseDataModelDTO) {
		FileInformation fileInfo = new FileInformation();

		if(baseDataModelDTO.getFileType().equals("drl")) {
			//fileInfo.setFileName(baseDataModelDTO.getFileName() + ".drl");
			fileInfo.setFileType("dmn");
		} else if(baseDataModelDTO.getFileType().equals("form")) {
			//fileInfo.setFileName(baseDataModelDTO.getFileName() + ".frm");
			fileInfo.setFileType("form");
		} else {
			fileInfo.setFileName(baseDataModelDTO.getFileName() + "." + baseDataModelDTO.getFileType());
			fileInfo.setFileType(baseDataModelDTO.getFileType());
		}
		if(baseDataModelDTO.getFileContent() != null) {
			fileInfo.setContent(new String(baseDataModelDTO.getFileContent()));
		}
		fileInfo.setFileName(baseDataModelDTO.getFileName());
		fileInfo.setWorkspaceName(baseDataModelDTO.getWorkspaceName());
		fileInfo.setMiniApp(baseDataModelDTO.getMiniAppName());
		if(baseDataModelDTO.getComment() != null) {
			fileInfo.setComment(baseDataModelDTO.getComment());
		} else {
			fileInfo.setComment("Updated " + baseDataModelDTO.getFileName() + " in Workspace");
		}

		return extensionService.updateFileInRepository(fileInfo);
	}

	public abstract Uni<EventResponseModel> bind(BasePropertyDto bpmnPropertyDto);

	public abstract void validate();

	public abstract Uni<EventResponseModel> generateFile(BaseDataModelDTO baseDataModelDTO, String flag, String userId);

	public abstract Uni<EventResponseModel> dataExtractor(FileModelDto fileModelDto);

	public abstract Uni<EventResponseModel> createMultiple(BaseModelList baseModels, String flag, String userId);

	public abstract Uni<EventResponseModel> bindMultiple(BindPropertyList propertyList);

}
