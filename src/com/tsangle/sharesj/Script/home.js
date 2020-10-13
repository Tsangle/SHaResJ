var init = function () {
    var fileInput = $("#fileInput");
    var selectFileButton = $("#selectFileButton");
    var fileNameInput = $("#fileNameInput");
    var uploadButton = $("#uploadButton");
    var cancelUploadButton = $("#cancelUploadButton");
    var selectFileDiv = $("#selectFileDiv");
    var uploadProgressDiv = $("#uploadProgressDiv");
    var uploadProgressBar = $("#uploadProgressBar");
    var uploadModal = $("#uploadModal");
    var networkSpeedDiv = $("#networkSpeedDiv");
    var fileListTableBody = $("#FileListTableBody");
    var pathBreadCrumb = $("#PathBreadCrumb");
    var navbarCollapse = $("#navbarCollapse");
    var alertModal = $("#alertModal");
    var alertModalBody = $("#alertModalBody");
    var fileModal = $("#fileModal");
    var fileModalBody = $("#fileModalBody");
    var deleteButton = $("#deleteButton");
    var downloadButton = $("#downloadButton");
    var fileListSpinner = $("#fileListSpinner");
    var deviceSizeDetector = $("#deviceSizeDetector");
    var navCollapseButton = $("#navCollapseButton");
    var navbarCollapseMask = $("#navbarCollapseMask");
    var previousFileButton = $("#previousFileButton");
    var nextFileButton = $("#nextFileButton");
    var navbarExpandIcon = $("#navbarExpandIcon");
    var navbarCloseIcon = $("#navbarCloseIcon");
    var floatingButton = $("#floatingButton");
    var floatingButtonIcon = $("#floatingButtonIcon");
    var creatingButton = $("#creatingButton");
    var uploadingButton = $("#uploadingButton");
    var settingButton = $("#settingButton");
    var settingModal = $("#settingModal");
    var threadNumberSelect = $("#threadNumberSelect");
    var acceptSettingButton = $("#acceptSettingButton");
    var cancelSettingButton = $("#cancelSettingButton");
    var body = $("body");
    var fileUploader;
    var filenameList;
    var currentFileIndex;

    var resetFileModalBody = function(){
        var video = document.getElementById("fileModalVideo");
        if (video !== null) {
            video.pause();
            video.innerHTML="";
            video.load();
        }
        fileModalBody.html("");
    };

    fileModal.on('hidden.bs.modal', function () {
        resetFileModalBody();
    });
    var alertMessage = function (message, type = "Info", style = "info") {
        $("<div class='alert alert-" + style + " alert-dismissable fade show' role='alert'>" +
            "<strong>" + type + ": </strong>" + message +
            "</div>").appendTo(alertModalBody).hide().fadeIn();
        alertModal.modal("show");
    };
    alertModal.on('hidden.bs.modal', function () {
        alertModalBody.html("");
    });
    var initUploadModal = function () {
        selectFileDiv.hide();
        uploadProgressBar.css("width", "0%");
        networkSpeedDiv.html("0 B/s");
        uploadProgressDiv.show();
        uploadButton.attr("disabled", true);
        uploadButton.text("Uploading...")
    }
    var showSpinnerInUploadModal = function () {
        uploadButton.text("Waiting...")
        networkSpeedDiv.html("<div class='spinner-border text-secondary spinner-border-sm' role='status'></div>");
    };
    var resetUploadModal = function () {
        fileUploader = undefined;
        uploadModal.modal('hide');
        selectFileDiv.show();
        uploadProgressDiv.hide();
        uploadProgressBar.css("width", "0%");
        fileInput.val("");
        fileNameInput.val("");
        networkSpeedDiv.html("0 B/s");
        uploadButton.attr("disabled", false);
        uploadButton.text("Upload")
    };
    var notifyUploadResult = function (completedPercentage, networkSpeed) {
        uploadProgressBar.css("width", completedPercentage + "%");
        networkSpeedDiv.html(networkSpeed);
    };
    var FileUploader = class {
        constructor(file, path, fileName, threadCount, notifyFunc) {
            this.file = file;
            this.path = path;
            this.fileName = fileName;
            this.threadCount = threadCount;
            this.errorDetected = false;
            this.chunkSize = (file.size - file.size % threadCount)/threadCount;
            if(this.chunkSize===0){
                this.chunkSize = 1;
                this.threadCount = file.size;
            }
            this.notifyFunc = notifyFunc;
            this.serverCacheID = 0;
            this.isCanceled = false;
            this.uploadProgress = 0;
        }

        startUpload() {
            var fileInfo = this.path + "|" + this.fileName + "|" + this.file.size;
            var thisObj = this;
            $.post("/File/SetFileInfo", fileInfo, function (data) {
                if (data.slice(0, 1) !== "#") {
                    thisObj.serverCacheID = data;
                    for (var index = 0; index < thisObj.threadCount; index++) {
                        thisObj._uploadChunk(index,thisObj);
                    }
                    thisObj._checkUploadProgress(thisObj, 0, "0 B/s");
                } else {
                    resetUploadModal();
                    alertMessage(data, "Error", "danger");
                }
            });
        }

        cancelUpload(){
            this._cancelUploadImpl(this);
        }

        _cancelUploadImpl(thisObj){
            if (!thisObj.isCanceled){
                thisObj.isCanceled = true;
                $.post("/File/CancelUpload", thisObj.serverCacheID, function (data) {
                    resetUploadModal();
                    if (data.slice(0, 1) !== "#") {
                        alertMessage("Uploading task canceled!", "Info", "success");
                    } else {
                        alertMessage(data, "Error", "danger");
                    }
                });
            }
        }

        _checkUploadProgress(thisObj, lastTimeStamp, lastSpeed){
            $.post("/File/CheckUploadProgress", thisObj.serverCacheID + "|" + thisObj.uploadProgress + "|" + lastTimeStamp, function (data) {
                if (data.slice(0, 1) !== "#") {
                    var dataArray = data.split("|");
                    thisObj.uploadProgress = parseFloat(dataArray[0]);
                    var timeStamp = Number(dataArray[1]);
                    var speed = dataArray[2];
                    var status = Number(dataArray[3]);
                    if(lastTimeStamp===timeStamp) {
                        speed = lastSpeed;
                    }
                    thisObj.notifyFunc(thisObj.uploadProgress, speed);
                    if(status===0){
                        thisObj._checkUploadProgress(thisObj, timeStamp, speed);
                    }else{
                        thisObj._waitForUploadCompletion(thisObj);
                    }
                } else {
                    if(!thisObj.isCanceled && !thisObj.errorDetected){
                        thisObj.errorDetected = true;
                        alertMessage(data, "Error", "danger");
                        thisObj._cancelUploadImpl(thisObj);
                    }
                }
            });
        }

        _waitForUploadCompletion(thisObj){
            showSpinnerInUploadModal();
            $.post("/File/WaitForUploadCompletion", thisObj.serverCacheID, function (data) {
                if (data.slice(0, 1) !== "#") {
                    resetUploadModal();
                    alertMessage("[" + thisObj.fileName + "] successfully uploaded!", "Info", "success");
                    refreshFileSystemEntryList();
                } else {
                    if(!thisObj.isCanceled && !thisObj.errorDetected){
                        thisObj.errorDetected = true;
                        alertMessage(data, "Error", "danger");
                        thisObj._cancelUploadImpl(thisObj);
                    }
                }
            });
        }

        _uploadChunk(index, thisObj) {
            var currentChunk;
            if (index === thisObj.threadCount - 1) {
                currentChunk = thisObj.file.slice(index * thisObj.chunkSize, thisObj.file.size);
            } else {
                currentChunk = thisObj.file.slice(index * thisObj.chunkSize, (index + 1) * thisObj.chunkSize);
            }
            var httpRequest = new XMLHttpRequest();
            httpRequest.overrideMimeType("text/xml");
            httpRequest.open("POST", "/File/UploadFileChunk/"+thisObj.serverCacheID+"&"+index * thisObj.chunkSize);
            httpRequest.setRequestHeader("Content-Type", "text/plain;charset=utf-8");
            httpRequest.onerror = function () {
                alertMessage("Unknown error encountered while uploading Chunk #" + index + " of [" + thisObj.fileName + "]", "Error", "danger");
                thisObj._cancelUploadImpl(thisObj);
            };
            httpRequest.send(currentChunk);
        }
    };
    selectFileButton.click(function () {
        fileInput.trigger("click");
    });
    uploadButton.click(function () {
        if (fileInput[0].files.length > 0) {
            initUploadModal()
            var file = fileInput[0].files[0];
            var uploadThreadCount = Number(localStorage.getItem("threadNumber"));
            fileUploader = new FileUploader(file, sessionStorage.getItem("path"), fileNameInput.val(), uploadThreadCount, notifyUploadResult);
            fileUploader.startUpload();
        } else {
            alertMessage("Please select a file!", "Warning", "warning");
        }
    });
    cancelUploadButton.click(function () {
        if (fileUploader !== undefined) {
            fileUploader.cancelUpload();
        } else {
            resetUploadModal();
        }
    });
    deleteButton.click(function (event) {
        if (deleteButton.attr("aria-describedby")!==undefined){
            deleteButton.tooltip('hide');
            var fullPath = sessionStorage.getItem("path") + "/" + $("#selectedFileName").text();
            if (sessionStorage.getItem("path") === "") {
                fullPath = $("#selectedFileName").text();
            }
            $.post("/File/DeleteFile", fullPath, function (data) {
                if (data.slice(0, 1) !== "#") {
                    fileModal.modal("hide");
                    refreshFileSystemEntryList();
                } else {
                    alertMessage(data, "Error", "danger");
                }
            });
        }else{
            deleteButton.tooltip('show');
        }
        event.stopPropagation();
    });
    fileModal.click(function(){
        deleteButton.tooltip('hide');
    });
    downloadButton.click(function () {
        var fullPath = sessionStorage.getItem("path") + "/" + $("#selectedFileName").text();
        if (sessionStorage.getItem("path") === "") {
            fullPath = $("#selectedFileName").text();
        }
        var downloadForm = $("<form>");
        downloadForm.attr('style', 'display:none');
        downloadForm.attr('target', '');
        downloadForm.attr('method', 'post');
        downloadForm.attr('action', "/File/DownloadFile");
        downloadForm.attr('target', '');

        var nameInput = $('<input>');
        nameInput.attr('type', 'hidden');
        nameInput.attr('name', 'fileName');
        nameInput.attr('value', fullPath);

        $('body').append(downloadForm);
        downloadForm.append(nameInput);
        downloadForm.submit();
        downloadForm.remove();
    });
    fileInput.change(function () {
        if (fileInput[0].files.length > 0) {
            fileNameInput.val(fileInput[0].files[0].name);
        } else {
            fileNameInput.val("");
        }
    });
    var getFileSystemEntry = function (path) {
        fileListTableBody.empty();
        fileListSpinner.show();
        $.post("/File/GetFileSystemEntries", path, function (data) {
            if (data.slice(0,1)!=="#") {
                sessionStorage.setItem("path", path);
                var pathNodeArray;
                if (path !==""){
                    pathNodeArray = path.split("/");
                    pathNodeArray.unshift("Root");
                } else {
                    pathNodeArray = ["Root"];
                }
                pathBreadCrumb.empty();
                for(var index = 0; index < pathNodeArray.length - 1; index++) {
                    pathBreadCrumb.append("<li class='breadcrumb-item'><a class='pathBreadCrumbFoldrName'>" + pathNodeArray[index] + "</a></li>");
                }
                pathBreadCrumb.append("<li class='breadcrumb-item active'><a>" + pathNodeArray[pathNodeArray.length - 1] + "</a></li>");
                $("a.pathBreadCrumbFoldrName").click(function () {
                    var length = $("a.pathBreadCrumbFoldrName").index(this) + 1;
                    var targetPath = "";
                    for (var index = 1; index < length; index++) {
                        targetPath += $("a.pathBreadCrumbFoldrName").eq(index).text();
                        if (index < length - 1) {
                            targetPath += "/";
                        }
                    }
                    getFileSystemEntry(targetPath);
                });
                fileListSpinner.hide();
                var fileSystemEntries = data.split("|");
                filenameList = [];
                for (var index = 0; index < fileSystemEntries.length - 1; index++) {
                    var entryInfo = fileSystemEntries[index].split("*");
                    var dateTime = entryInfo[1].split(" ");
                    if (entryInfo[2] === "") {
                        $("<tr class='entryTableFolder'><td><div class='float-left mr-1' style='width:1rem;text-align:center;'>" +
                            "<i class='folderIcon fas fa-folder' aria-hidden='true'></i></div>" +
                            entryInfo[0] + "</td><td><div style='float:left;margin-right:10px;'>" +
                            dateTime[0] + "</div><div class='d-none d-md-block' style='float:left;'>" +
                            dateTime[1] + "</div></td><td>" +
                            entryInfo[2] + "</td></tr>").appendTo(fileListTableBody).hide().fadeIn();
                    } else {
                        var fileSizeDisplay = parseFloat(entryInfo[2]);
                        var unit = 'KB';
                        if (fileSizeDisplay / 1024 > 1) {
                            fileSizeDisplay = fileSizeDisplay / 1024;
                            unit = 'MB';
                            if (fileSizeDisplay / 1024 > 1) {
                                fileSizeDisplay = fileSizeDisplay / 1024;
                                unit = 'GB';
                                if (fileSizeDisplay / 1024 > 1) {
                                    fileSizeDisplay = fileSizeDisplay / 1024;
                                    unit = 'TB';
                                }
                            }
                        }
                        $("<tr class='entryTableFile'><td index='" + filenameList.length + "'><div class='float-left mr-1' style='width:1rem;text-align:center;'>" +
                            "<i class='fileIcon far fa-file-alt' aria-hidden='true'></i></div>" +
                            entryInfo[0] + "</td><td><div style='float:left;margin-right:10px;'>" +
                            dateTime[0] + "</div><div class='d-none d-md-block' style='float:left;'>" +
                            dateTime[1] + "</div></td><td><div style='float:left;'>" +
                            Math.round(fileSizeDisplay) + " " + unit + "</div></td></tr>").appendTo(fileListTableBody).hide().fadeIn();
                        filenameList.push(entryInfo[0]);
                    }
                }
                $(function () {
                    $("[data-toggle='tooltip']").tooltip();
                });

                $("tr.entryTableFolder").click(function () {
                    folderName = $(this).children("td").first().text();
                    if (sessionStorage.getItem("path") === "") {
                        getFileSystemEntry(folderName);
                    } else {
                        getFileSystemEntry(sessionStorage.getItem("path") + "/" + folderName);
                    }
                });

                $("tr.entryTableFile").click(function () {
                    currentFileIndex = Number($(this).children("td").first().attr("index"));
                    showFileInfo($(this).children("td").first().text());
                });
            } else {
                alertMessage(data, "Error", "danger");
            }

        });
    };
    var refreshFileSystemEntryList = function () {
        getFileSystemEntry(sessionStorage.getItem("path"));
    };
    var showFileInfo = function (fileName) {
        previousFileButton.css("opacity", 1);
        nextFileButton.css("opacity", 1);
        if (currentFileIndex===0){
            previousFileButton.css("opacity", 0);
        }
        if(currentFileIndex===filenameList.length-1){
            nextFileButton.css("opacity", 0);
        }
        var fileNameArray = fileName.split(".");
        var extendName = fileNameArray[fileNameArray.length - 1];
        if (extendName === "mp4" || extendName === "MP4") {
            fileModalBody.html("<video id='fileModalVideo' width='100%' height='auto'controls preload='none'>" +
                "<source src='/Video/PlayVideo/" + encodeURIComponent(sessionStorage.getItem("path") + "/" + fileName) + "' type='video/mp4'></video>" +
                "<div id='selectedFileName' class='fileInfo text-break'>" + fileName + "</div>");
        } else if (extendName === "JPG" || extendName === "PNG" || extendName === "GIF" ||
            extendName === "jpg" || extendName === "png" || extendName === "gif") {
            fileModalBody.html("<img src='/Image/DisplayImage/" + encodeURIComponent(sessionStorage.getItem("path") + "/" + fileName) + "' class='img-thumbnail'>" +
                "<div id='selectedFileName' class='fileInfo text-break'>" + fileName + "</div>");
        }else {
            fileModalBody.html("<div class='fileInfo' style='font-size:90px;color:rgb(130, 130, 130);'>" +
                "<span class='fas fa-file-alt' aria-hidden='true'></span></div>" +
                "<div id='selectedFileName' class='fileInfo text-break'>" + fileName + "</div>");
        }
        fileModal.modal("show");
    };

    previousFileButton.click(function(){
        if (currentFileIndex>0){
            currentFileIndex--;
            resetFileModalBody();
            showFileInfo(filenameList[currentFileIndex]);
        }
    });

    nextFileButton.click(function(){
        if (currentFileIndex<filenameList.length-1){
            currentFileIndex++;
            resetFileModalBody();
            showFileInfo(filenameList[currentFileIndex]);
        }
    });

    navbarCollapse.on("show.bs.collapse", function(){
        navbarCollapseMask.fadeIn();
        navbarExpandIcon.fadeOut();
        navbarCloseIcon.fadeIn();
        body.addClass("navbarCollapseMaskOpen");
    });

    navbarCollapse.on("hide.bs.collapse", function(){
        navbarCollapseMask.fadeOut();
        navbarExpandIcon.fadeIn();
        navbarCloseIcon.fadeOut();
        body.removeClass("navbarCollapseMaskOpen");
    });

    navbarCollapseMask.click(function(){
        if (navCollapseButton.is(":visible")){
            navbarCollapse.collapse('hide');
        } else {
            navbarCollapse.removeClass("show");
            navbarCollapseMask.fadeOut();
            navbarExpandIcon.show();
            navbarCloseIcon.hide();
            body.removeClass("navbarCollapseMaskOpen");
        }
    });

    var switchFloatingButton = function(){
        if(floatingButtonIcon.hasClass("floatingButtonIconExpand")){
            floatingButtonIcon.removeClass("floatingButtonIconExpand");
            creatingButton.removeClass("creatingButtonExpand");
            uploadingButton.removeClass("uploadingButtonExpand");
            settingButton.removeClass("settingButtonExpand");
        }else {
            floatingButtonIcon.addClass("floatingButtonIconExpand");
            creatingButton.addClass("creatingButtonExpand");
            uploadingButton.addClass("uploadingButtonExpand");
            settingButton.addClass("settingButtonExpand");
        }
    };

    floatingButton.click(function(){
        switchFloatingButton();
    });

    creatingButton.click(function(){
        switchFloatingButton();
    });

    uploadingButton.click(function(){
        switchFloatingButton();
        uploadModal.modal("show");
    });

    settingButton.click(function(){
        switchFloatingButton();
        settingModal.modal("show");
    });

    var initSetting = function(){
        var threadNumber = localStorage.getItem("threadNumber");
        if (threadNumber===null){
            threadNumber="1";
            localStorage.setItem("threadNumber", threadNumber);
        }
        threadNumberSelect.val(threadNumber);
    };

    settingModal.on('show.bs.modal', function(){
        initSetting();
    });

    acceptSettingButton.click(function(){
        localStorage.setItem("threadNumber", threadNumberSelect.val());
        settingModal.modal("hide");
    });

    cancelSettingButton.click(function(){
        initSetting();
        settingModal.modal("hide");
    });

    initSetting();
    var currentPath = sessionStorage.getItem("path");
    if (currentPath !== null && currentPath !== undefined) {
        getFileSystemEntry(currentPath);
    } else {
        getFileSystemEntry("");
    }
};

var setCookie = function (name, value) {
    var Days = 30;
    var exp = new Date();
    exp.setTime(exp.getTime() + Days * 24 * 60 * 60 * 1000);
    document.cookie = name + "=" + encodeURI(value) + ";expires=" + exp.toGMTString();
};

init();