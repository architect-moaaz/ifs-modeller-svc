# API REFERENCE - App Upgrade

url = “https://api.intelliflow.in/modeler/modellerService”

## Workspace API's

### Create Workspace
Method : POST – url/createWorkspace

Payload : 

        workspaceName : Name of the Workspace to be created

Response :

        Success (200 OK Status):
            Workspace has been created successfully
        Failure (4xx or 5xx):
            Error Creating the Workspace


### List Workspaces
Method : GET – url/listWorkspaces

Response :

        Success (200 OK Status):
            Provides List of All Workspaces Created
        Failure (4xx or 5xx):
            Error Fetching the data


### Delete Workspace
Method : DELETE – url/deleteWorkspace

Payload :

        workspaceName : Name of the Workspace to be deleted
        userId : username of the initiator

Response :

        Success (200 OK Status):
            Workspace has been deleted successfully
        Failure (4xx or 5xx):
            Error Deleting the Workspace


### Get Data In Workspace
Method : GET – url/{workspacename}/data

Path Paramter : 
    
        workspacename : Name of the workspace you need to fetch the data of

Query Parameters :

        sort : Sort Criteria to sort the fetched data
        filter : filters (appname) to filter the fetched data
        status : status based fetch on the data
        page : page number of paginated data
        size : number of app data to be shown in each page

Response :

        Success (200 OK Status):
            Workspace has been fetched successfully
        Failure (4xx or 5xx):
            Error fetching the Workspace


## App (Miniapp) API's

### Create MiniApp (App Repository)
Method : POST – url/createRepository

Payload :

        workspaceName : Name of the Workspace in which app is to be created
        miniAppName: Name of the App to be created
        description:  App description text
        deviceSupport: Device support app will provide (M - Mobile /D - Desktop / B - Both )
        logoURL: Url at which the logo of the application is store in CDS
        colorScheme: Coloscheme the application is expected to use

Response :

        Success (200 OK Status):
            App has been created successfully
        Failure (4xx or 5xx):
            Error Creating the App


### List Mini Apps
Method : POST – url/listMiniApps

Payload :

        workspaceName : Name of the Workspace from which mini apps is to be fetched

Response :

        Success (200 OK Status):
            Apps has been fetched successfully
        Failure (4xx or 5xx):
            Error fetching the App


### Get All Resources (Files) inside an App
Method : POST – url/getResources

Payload :

        workspaceName : Name of the Workspace from which mini app is to be fetched
        miniAppName: Name of the App from ehich resources is to be fetched

Response :

        Success (200 OK Status):
            Apps data has been fetched successfully
        Failure (4xx or 5xx):
            Error fetching the App data


### Delete Mini App
Method : Delete – url/getResources

Payload :

        workspaceName : Name of the Workspace from which mini app is to be deleted
        miniAppName: Name of the App to be deleted

Response :

        Success (200 OK Status):
            Apps has been deleted successfully
        Failure (4xx or 5xx):
            Error deleting the App


### Get App Information
Method : Get – url/{workspacename}/{appname}/info

Payload :

        workspaceName : Name of the Workspace from which mini app is to be fetched
        miniAppName: Name of the App to be fetched

Query Param:

        status: status of the app being fetched

Response :

        Success (200 OK Status):
            App information has been fetched successfully
        Failure (4xx or 5xx):
            Error fetching the App data

### Get App Information
Method : Get – url/cloneApplication

Payload :

        sourceworkspaceName: Name of workspace from where app is to be cloned
        destworkspaceName: Name of workspace to which the new app is created
        sourceminiApp: Name of the app to be cloned
        destminiApp: Name of the app newly created
        filedesc: Optional Parameter to selected specific file/files
        deviceSupport: Device support for the new app
        colorScheme : Colo scheme for the newly created app

Response :

        Success (200 OK Status):
            App has been cloned successfully
        Failure (4xx or 5xx):
            Error cloning the App

### Get App Information
Method : Get – url/cloneApplication

Payload :

        sourceworkspaceName: Name of workspace from where app is to be cloned
        destworkspaceName: Name of workspace to which the new app is created
        sourceminiApp: Name of the app to be cloned
        destminiApp: Name of the app newly created
        filedesc: Optional Parameter to selected specific file/files
        deviceSupport: Device support for the new app
        colorScheme : Colo scheme for the newly created app

Response :

        Success (200 OK Status):
            App has been cloned successfully
        Failure (4xx or 5xx):
            Error cloning the App

## File API's

### Create File in App
Method : POST – url/{modeller}/createFile

   Path Parameter :  

        modeller : Name of the Modeller according to type i.e. (formmodeller/datamodeller/dmnmodeller/bpmnmodeller)

   Payload :

        workspaceName : Name of the Workspace it belongs too
        miniAppName : Name of the MiniApp name
        fileName : Name of the file to be created
        fileContent: Content of the file
        fileType : Type of file to be created i.e. form/datamodel/bpmn/dmn
        comment: Optional Comment to be stored as part of the file creation

   Response :
   
        Success (200 OK Status):
            File has been created successfully
        Failure (4xx or 5xx):
            Error Creating the file

### Update File in App
Method : POST – url/{modeller}/updateFile

Path Parameter :

        modeller : Name of the Modeller according to type i.e. (formmodeller/datamodeller/dmnmodeller/bpmnmodeller)

Payload :

        workspaceName : Name of the Workspace it belongs too
        miniAppName : Name of the MiniApp name
        fileName : Name of the file to be updated
        fileContent: Content of the file
        fileType : Type of file to be updated i.e. form/datamodel/bpmn/dmn
        comment: Optional Comment to be stored as part of the file creation

Response :

        Success (200 OK Status):
            File has been updated successfully
        Failure (4xx or 5xx):
            Error updating the file


### Generate File with data from another in App
Method : POST – url/{modeller}/generateFile

Path Parameter :

        modeller : Name of the Modeller according to type i.e. (formmodeller/datamodeller/dmnmodeller/bpmnmodeller)

Query Paramter: 
    
        flag : flag variable passed for generation options
Payload :

        workspaceName : Name of the Workspace it belongs too
        miniAppName : Name of the MiniApp name
        fileName : Name of the file to be created
        fileType : Type of file to be created i.e. form/datamodel/bpmn/dmn
        comment: Optional Comment to be stored as part of the file creation

Response :

        Success (200 OK Status):
            File has been generated successfully
        Failure (4xx or 5xx):
            Error generating the file


### Fetch File Information 
Method : POST – url/fetchFile/{type}

Path Parameter :

        type : content/meta of the file to be fetched

Payload :

        workspaceName : Name of the Workspace it belongs too
        miniAppName : Name of the MiniApp name
        fileName : Name of the file to be created
        fileType : Type of file to be created i.e. form/datamodel/bpmn/dmn
        comment: Optional Comment to be stored as part of the file creation

Response :

        Success (200 OK Status):
            File information has been fetched successfully
        Failure (4xx or 5xx):
            Error fetching the file information


### Bind Files to Another
Method : POST – url/{modellerType}/bind

Path Parameter :

        modeller : Name of the Modeller according to type i.e. (formmodeller/datamodeller/dmnmodeller/bpmnmodeller)

Payload :

        workspaceName : Name of the Workspace it belongs too
        miniAppName : Name of the MiniApp name
        fileName : Name of the file to be created
        fileType : Type of file to be created i.e. form/datamodel/bpmn/dmn
        comment: Optional Comment to be stored as part of the file creation

Response :

        Success (200 OK Status):
            File has been generated successfully
        Failure (4xx or 5xx):
            Error generating the file