var initSetting=function(){
    var settingModal=$("#settingModal");
    var folderSelectModal=$("#folderSelectModal");
    var addSharedFolderButton=$("#addSharedFolderButton");

    addSharedFolderButton.click(function(){
        folderSelectModal.modal("show");
    });
};

initSetting();