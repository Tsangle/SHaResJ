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
    var navbarCollapse = $("#navbar-collapse");
    var alertModal = $("#alertModal");
    var alertModalBody = $("#alertModalBody");
    var fileModal = $("#fileModal");
    var fileModalBody = $("#fileModalBody");
    var deleteButton = $("#deleteButton");
    var downloadButton = $("#downloadButton");
    var fileListSpinner = $("#fileListSpinner");
    var deviceSizeDetector = $("#deviceSizeDetector")
    var navCollapseButton = $("#navCollapseButton")
    var fileUploader;

    fileModal.on('hidden.bs.modal', function () {
        var video = document.getElementById("fileModalVideo");
        if (video !== null) {
            video.pause();
            video.innerHTML="";
            video.load();
        }
        fileModalBody.html("");
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
    var resetUpload = function () {
        fileUploader = undefined;
        uploadModal.modal('hide');
        selectFileDiv.show();
        uploadProgressDiv.hide();
        uploadProgressBar.css("width", "0%");
        fileInput.val("");
        fileNameInput.val("");
        networkSpeedDiv.html("0 B/s");
        uploadButton.attr("disabled", false);
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
                    resetUpload();
                    alertMessage(data, "Error", "danger");
                }
            });
        }

        cancelUpload(){
            var thisObj = this;
            thisObj.isCanceled = true;
            $.post("/File/CancelUpload", thisObj.serverCacheID, function (data) {
                resetUpload();
                if (data.slice(0, 1) !== "#") {
                    alertMessage("Uploading task canceled!", "Info", "success");
                } else {
                    alertMessage(data, "Error", "danger");
                }
            });
        }

        _checkUploadProgress(thisObj, time, currentSpeed){
            $.post("/File/CheckUploadProgress", thisObj.serverCacheID + "|" + thisObj.uploadProgress + "|" + time, function (data) {
                if (data.slice(0, 1) !== "#") {
                    var dataArray = data.split("|");
                    thisObj.uploadProgress = parseFloat(dataArray[0]);
                    var timeStamp = Number(dataArray[1]);
                    var speed = dataArray[2];
                    var status = Number(dataArray[3]);
                    if(status===0){
                        if(time===timeStamp){
                            thisObj.notifyFunc(thisObj.uploadProgress, currentSpeed);
                            thisObj._checkUploadProgress(thisObj, timeStamp, currentSpeed);
                        }else{
                            thisObj.notifyFunc(thisObj.uploadProgress, speed);
                            thisObj._checkUploadProgress(thisObj, timeStamp, speed);
                        }
                    }else{
                        resetUpload();
                        alertMessage("Uploading task completed!", "Info", "success");
                        refreshFileSystemEntryList();
                    }
                } else {
                    if(!thisObj.isCanceled && !thisObj.errorDetected){
                        thisObj.errorDetected = true;
                        alertMessage(data, "Error", "danger");
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
            httpRequest.send(currentChunk);
        }
    };
    selectFileButton.click(function () {
        fileInput.trigger("click");
    });
    uploadButton.click(function () {
        if (fileInput[0].files.length > 0) {
            selectFileDiv.hide();
            uploadProgressBar.css("width", "0%");
            uploadProgressDiv.show();
            uploadButton.attr("disabled", true);
            var file = fileInput[0].files[0];
            var uploadThreadCount = 5;
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
            resetUpload();
        }
    });
    deleteButton.click(function () {
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
                for (var index = 0; index < fileSystemEntries.length - 1; index++) {
                    var entryInfo = fileSystemEntries[index].split("*");
                    var dateTime = entryInfo[1].split(" ");
                    if (entryInfo[2] === "") {
                        $("<tr><td><i class='folderIcon fa fa-folder-open' aria-hidden='true'></i><a class='entryTableFolderName'>" +
                            entryInfo[0] + "</a></td><td><div data-toggle='tooltip' data-placement='top' title='" +
                            dateTime[0] + " " + dateTime[1] + "' style='float:left;'><div style='float:left;margin-right:10px;'>" +
                            dateTime[0] + "</div><div class='d-none d-md-block' style='float:left;'>" +
                            dateTime[1] + "</div></div></td><td>" +
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
                        $("<tr><td class='tableFileName'><i class='fileIcon fa fa fa-file-o' aria-hidden='true'></i>" +
                            entryInfo[0] + "</td><td><div data-toggle='tooltip' data-placement='top' title='" +
                            dateTime[0] + " " + dateTime[1] + "' style='float:left;'><div style='float:left;margin-right:10px;'>" +
                            dateTime[0] + "</div><div class='d-none d-md-block' style='float:left;'>" +
                            dateTime[1] + "</div></div></td><td><div data-toggle='tooltip' data-placement='top' title='" +
                            entryInfo[2] + " KB' style='float:left;'>" +
                            Math.round(fileSizeDisplay) + " " + unit + "</div></td></tr>").appendTo(fileListTableBody).hide().fadeIn();
                    }
                }
                $(function () {
                    $("[data-toggle='tooltip']").tooltip();
                });

                $("a.entryTableFolderName").click(function () {
                    if (sessionStorage.getItem("path") === "") {
                        getFileSystemEntry($(this).text());
                    } else {
                        getFileSystemEntry(sessionStorage.getItem("path") + "/" + $(this).text());
                    }
                });

                $("td.tableFileName").click(function () {
                    showFileInfo($(this).text());
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
        var fileNameArray = fileName.split(".");
        var extendName = fileNameArray[fileNameArray.length - 1];
        if (extendName === "mp4" || extendName === "MP4") {
            fileModalBody.html("<video id='fileModalVideo' width='100%' height='auto'controls preload='none'>" +
                "<source src='/Video/PlayVideo/" + sessionStorage.getItem("path") + "/" + fileName +
                "' type='video/mp4'></video>" +
                "<div id='selectedFileName' class='fileInfo'>" + fileName + "</div>");
        } else if (extendName === "JPG" || extendName === "PNG" || extendName === "GIF" ||
            extendName === "jpg" || extendName === "png" || extendName === "gif") {
            fileModalBody.html("<img src='/Image/DisplayImage/" + sessionStorage.getItem("path") + "/" + fileName + "' class='img-thumbnail'>" +
                "<div id='selectedFileName' class='fileInfo'>" + fileName + "</div>");
        }else {
            fileModalBody.html("<div class='fileInfo' style='font-size:80px;color:rgb(190, 190, 190);'>" +
                "<span class='fa fa-download' aria-hidden='true'></span></div>" +
                "<div id='selectedFileName' class='fileInfo'>" + fileName + "</div>");
        }
        fileModal.modal("show");
    };

    isNavCollapseButtonVisible = navCollapseButton.is(":visible")

    $(window).on("resize", function(){
        if (navCollapseButton.is(":hidden") && isNavCollapseButtonVisible){
            navbarCollapse.removeClass("show");
        }
        isNavCollapseButtonVisible = navCollapseButton.is(":visible")
    })

    $("div.navItemFont").click(function(){
        if (navCollapseButton.is(":visible")){
            navbarCollapse.collapse('hide');
        }
    });

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
    document.cookie = name + "=" + escape(value) + ";expires=" + exp.toGMTString();
};

init();