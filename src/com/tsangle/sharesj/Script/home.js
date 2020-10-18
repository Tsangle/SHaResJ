var init = function () {
    var fileInput = $("#fileInput");
    var selectFileButton = $("#selectFileButton");
    var fileNameInput = $("#fileNameInput");
    var startUploadingButton = $("#startUploadingButton");
    var cancelUploadingButton = $("#cancelUploadingButton");
    var selectFileDiv = $("#selectFileDiv");
    var uploadingProgressDiv = $("#uploadingProgressDiv");
    var uploadingProgressBar = $("#uploadingProgressBar");
    var writingProgressBar = $("#writingProgressBar");
    var uploadingModal = $("#uploadingModal");
    var uploadingSpeedDiv = $("#uploadingSpeedDiv");
    var writingSpeedDiv = $("#writingSpeedDiv");
    var fileListTableBody = $("#FileListTableBody");
    var navbarCollapse = $("#navbarCollapse");
    var alertModal = $("#alertModal");
    var alertModalBody = $("#alertModalBody");
    var fileModal = $("#fileModal");
    var fileModalBody = $("#fileModalBody");
    var deleteButton = $("#deleteButton");
    var downloadButton = $("#downloadButton");
    var fileListSpinner = $("#fileListSpinner");
    var previousFileButton = $("#previousFileButton");
    var nextFileButton = $("#nextFileButton");
    var floatingButton = $("#floatingButton");
    var floatingButtonIcon = $("#floatingButtonIcon");
    var creatingButton = $("#creatingButton");
    var uploadingButton = $("#uploadingButton");
    var settingButton = $("#settingButton");
    var settingModal = $("#settingModal");
    var threadNumberSelect = $("#threadNumberSelect");
    var acceptSettingButton = $("#acceptSettingButton");
    var cancelSettingButton = $("#cancelSettingButton");
    var pathDropdownMenu = $("#pathDropdownMenu");
    var currentFolderNameElement = $("#currentFolderNameElement");
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
        $("<div class='alert alert-" + style + " alert-dismissable fade show text-break' role='alert'>" +
            "<strong>" + type + ": </strong>" + message +
            "</div>").appendTo(alertModalBody).hide().fadeIn();
        alertModal.modal("show");
    };
    alertModal.on('hidden.bs.modal', function () {
        alertModalBody.html("");
    });
    var initUploadingModal = function () {
        selectFileDiv.hide();
        uploadingProgressBar.css("width", "0%");
        uploadingSpeedDiv.html("0 B/s");
        writingProgressBar.css("width", "0%");
        writingSpeedDiv.html("0 B/s");
        uploadingProgressDiv.show();
        startUploadingButton.attr("disabled", true);
        startUploadingButton.text("Uploading...")
    }
    var resetUploadingModal = function () {
        fileUploader = undefined;
        uploadingModal.modal('hide');
        selectFileDiv.show();
        uploadingProgressDiv.hide();
        uploadingProgressBar.css("width", "0%");
        writingProgressBar.css("width", "0%");
        fileInput.val("");
        fileNameInput.val("");
        uploadingSpeedDiv.html("0 B/s");
        writingSpeedDiv.html("0 B/s");
        startUploadingButton.attr("disabled", false);
        startUploadingButton.text("Upload")
    };
    var notifyUploadingResult = function (uploadingProgress, uploadingSpeed, writingProgress, writingSpeed) {
        uploadingProgressBar.css("width", uploadingProgress + "%");
        if(uploadingProgress>=100){
            uploadingSpeedDiv.html("<i class='fas fa-check-circle' style='color:rgb(150,150,150);'></i>");
        }else{
            uploadingSpeedDiv.html(uploadingSpeed);
        }
        writingProgressBar.css("width", writingProgress + "%");
        if(writingProgress>=100){
            writingSpeedDiv.html("<i class='fas fa-check-circle' style='color:rgb(150,150,150);'></i>");
        }else{
            writingSpeedDiv.html(writingSpeed);
        }
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
            this.uploadingProgress = 0;
            this.writingProgress = 0;
        }

        startUploading() {
            var fileInfo = this.path + "|" + this.fileName + "|" + this.file.size;
            var thisObj = this;
            $.post("/File/SetFileInfo", fileInfo, function (data) {
                if (data.slice(0, 1) !== "#") {
                    thisObj.serverCacheID = data;
                    for (var index = 0; index < thisObj.threadCount; index++) {
                        thisObj._uploadChunk(index,thisObj);
                    }
                    thisObj._checkUploadingProgress(thisObj, 0, "0 B/s", "0 B/s");
                } else {
                    resetUploadingModal();
                    alertMessage(data, "Error", "danger");
                }
            });
        }

        cancelUploading(){
            this._cancelUploadingImpl(this);
        }

        _cancelUploadingImpl(thisObj){
            if (!thisObj.isCanceled){
                thisObj.isCanceled = true;
                $.post("/File/CancelUploading", thisObj.serverCacheID, function (data) {
                    resetUploadingModal();
                    if (data.slice(0, 1) !== "#") {
                        alertMessage("Uploading task canceled!", "Info", "success");
                    } else {
                        alertMessage(data, "Error", "danger");
                    }
                });
            }
        }

        _checkUploadingProgress(thisObj, lastTimeStamp, lastUploadingSpeed, lastWritingSpeed){
            $.post("/File/CheckUploadingProgress", thisObj.serverCacheID + "|" + thisObj.uploadingProgress + "|" + thisObj.writingProgress + "|" + lastTimeStamp, function (data) {
                if (data.slice(0, 1) !== "#") {
                    var dataArray = data.split("|");
                    thisObj.uploadingProgress = parseFloat(dataArray[0]);
                    thisObj.writingProgress = parseFloat(dataArray[1]);
                    var timeStamp = Number(dataArray[2]);
                    var uploadingSpeed = dataArray[3];
                    var writingSpeed = dataArray[4];
                    var status = Number(dataArray[5]);
                    if(lastTimeStamp===timeStamp) {
                        uploadingSpeed = lastUploadingSpeed;
                        writingSpeed = lastWritingSpeed;
                    }
                    thisObj.notifyFunc(thisObj.uploadingProgress, uploadingSpeed, thisObj.writingProgress, writingSpeed);
                    if(status===0){
                        thisObj._checkUploadingProgress(thisObj, timeStamp, uploadingSpeed, writingSpeed);
                    }else{
                        resetUploadingModal();
                        alertMessage("[" + thisObj.fileName + "] uploaded!", "Info", "success");
                        refreshFileSystemEntryList();
                    }
                } else {
                    if(!thisObj.isCanceled && !thisObj.errorDetected){
                        thisObj.errorDetected = true;
                        alertMessage(data, "Error", "danger");
                        thisObj._cancelUploadingImpl(thisObj);
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
                thisObj._cancelUploadingImpl(thisObj);
            };
            httpRequest.send(currentChunk);
        }
    };
    selectFileButton.click(function () {
        fileInput.trigger("click");
    });
    startUploadingButton.click(function () {
        if (fileInput[0].files.length > 0) {
            initUploadingModal()
            var file = fileInput[0].files[0];
            var uploadingThreadCount = Number(localStorage.getItem("threadNumber"));
            fileUploader = new FileUploader(file, sessionStorage.getItem("path"), fileNameInput.val(), uploadingThreadCount, notifyUploadingResult);
            fileUploader.startUploading();
        } else {
            alertMessage("Please select a file!", "Warning", "warning");
        }
    });
    cancelUploadingButton.click(function () {
        if (fileUploader !== undefined) {
            fileUploader.cancelUploading();
        } else {
            resetUploadingModal();
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
        sessionStorage.setItem("path", path);
        var pathNodeArray;
        if (path !==""){
            pathNodeArray = path.split("/");
            currentFolderNameElement.html("<i class='fas fa-caret-down'></i> " + pathNodeArray[pathNodeArray.length-1]);
        } else {
            pathNodeArray = [];
            currentFolderNameElement.html("<i class='fas fa-caret-down'></i> Home");
        }
        pathDropdownMenu.html("<span class='dropdown-item text-truncate pathDropdownMenuItem'  style='padding-left:1rem;'><i class='fas fa-hdd' style='color:rgb(150,150,150);margin-right:.5rem;'></i>Home</span>")
        for(var index = 0; index < pathNodeArray.length; index++) {
            pathDropdownMenu.append("<span class='dropdown-item text-truncate pathDropdownMenuItem' style='padding-left:" + (1 + 0.5*(index + 1)) + "rem'><i class='far fa-folder' style='color:rgb(150,150,150);margin-right:.5rem;'></i>" + pathNodeArray[index] + "</span>");
        }
        $("span.pathDropdownMenuItem").click(function () {
            var length = $("span.pathDropdownMenuItem").index(this) + 1;
            var targetPath = "";
            for (var index = 1; index < length; index++) {
                targetPath += $("span.pathDropdownMenuItem").eq(index).text();
                if (index < length - 1) {
                    targetPath += "/";
                }
            }
            getFileSystemEntry(targetPath);
        });
        fileListTableBody.empty();
        fileListSpinner.show();
        $.post("/File/GetFileSystemEntries", path, function (data) {
            if (data.slice(0,1)!=="#") {
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
        uploadingModal.modal("show");
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