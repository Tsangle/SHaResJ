﻿var init = function () {
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
    var mainNavbar = $(".mainNavbar");
    var alertModal = $("#alertModal");
    var alertModalBody = $("#alertModalBody");
    var fileModal = $("#fileModal");
    var fileModalBody = $("#fileModalBody");
    var deleteButton = $("#deleteButton");
    var downloadButton = $("#downloadButton");
    var fileListSpinner = $("#fileListSpinner");
    var previousFileButton = $("#previousFileButton");
    var nextFileButton = $("#nextFileButton");
    var floatingMenuContainer = $(".floatingMenuContainer");
    var toggleButton = $("#toggleButton");
    var toggleButtonIcon = $("#toggleButtonIcon");
    var creatingButton = $("#creatingButton");
    var uploadingButton = $("#uploadingButton");
    var settingButton = $("#settingButton");
    var settingModal = $("#settingModal");
    var threadNumberSelect = $("#threadNumberSelect");
    var acceptSettingButton = $("#acceptSettingButton");
    var cancelSettingButton = $("#cancelSettingButton");
    var pathDropdownMenu = $("#pathDropdownMenu");
    var navDropdown = $("#navDropdown");
    var currentFolderNameElement = $("#currentFolderNameElement");
    var dropdownIcon = $("#dropdownIcon");
    var navDropdownMask = $("#navDropdownMask");
    var floatingMenuMask = $("#floatingMenuMask");
    var folderNumberElement = $("#folderNumberElement");
    var fileNumberElement = $("#fileNumberElement");
    var fileModalLabel = $("#fileModalLabel");
    var confirmModal = $("#confirmModal");
    var confirmModalBody = $("#confirmModalBody");
    var confirmButton = $("#confirmButton");
    var cancelButton = $("#cancelButton");
    var confirmModalTitle = $("#confirmModalTitle");
    var creatingFolderModal = $("#creatingFolderModal");
    var createFolderButton = $("#createFolderButton");
    var cancelCreatingButton = $("#cancelCreatingButton");
    var folderNameInput = $("#folderNameInput");
    var optionMenuMask = $("#optionMenuMask");
    var propertyModal = $("#propertyModal");
    var nameInPropForm = $("#nameInPropForm");
    var pathInPropForm = $("#pathInPropForm");
    var urlInPropForm = $("#urlInPropForm");
    var lastModifiedTimeInPropForm = $("#lastModifiedTimeInPropForm");
    var typeInPropForm = $("#typeInPropForm");
    var sizeInPropForm = $("#sizeInPropForm");
    var renamingEntryModal = $("#renamingEntryModal");
    var newEntryNameInput = $("#newEntryNameInput");
    var renameEntryButton = $("#renameEntryButton");
    var bodyMask = $("#bodyMask");
    var body = $("body");
    var shownModalCount = 0;
    var fileUploader;
    var filenameList;
    var currentFileIndex;
    var selectedEntry;
    var serviceNodeName = currentFolderNameElement.text();

    $('.modal').on('show.bs.modal', function (e) {
        body.css("overflow-y", "hidden");
        shownModalCount++;
    });

    $('.modal').on('hide.bs.modal', function (e) {
        shownModalCount--;
        if (shownModalCount===0){
            body.css("overflow-y", "");
        }
    });

    navDropdown.on('show.bs.dropdown', function () {
        navDropdownMask.show();
        mainNavbar.css("z-index", "1000");
    });

    navDropdown.on('hide.bs.dropdown', function () {
        navDropdownMask.hide();
        mainNavbar.css("z-index", "");
    });

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
    var setMaxWidthForNav = function(){
        pathDropdownMenu.css("max-width", $("div.container").width()+"px");
        currentFolderNameElement.css("max-width", ($("div.container").width()-18)+"px");
    };
    $( window ).resize(function() {
        setMaxWidthForNav();
    });
    var showAlertMessage = function (message, type = "Info") {
        var style = "";
        switch(type){
            case "Info":
                style = "success"
                break;
            case "Warning":
                style = "warning"
                break;
            case "Error":
                style = "danger"
                break;
        }
        $("<div class='alert alert-" + style + " alert-dismissable fade show text-break' role='alert'>" +
            "<strong>" + type + ": </strong>" + message +
            "</div>").appendTo(alertModalBody).hide().fadeIn();
        alertModal.modal("show");
    };
    alertModal.on('hidden.bs.modal', function () {
        alertModalBody.html("");
    });
    var showConfirmationDialog = function (title, message, confirmCallback) {
        confirmModalTitle.html(title);
        confirmModalBody.html(message);
        confirmButton.click(confirmCallback);
        confirmButton.click(function(){
            confirmModal.modal("hide");
        });
        confirmModal.modal("show");
    };
    confirmModal.on('hidden.bs.modal', function () {
        confirmModalTitle.html("");
        confirmModalBody.html("");
    });
    confirmModal.on('hide.bs.modal', function(){
        confirmButton.off();
    });
    cancelButton.click(function(){
        confirmModal.modal("hide");
    });
    renamingEntryModal.on('show.bs.modal', function (e) {
        bodyMask.fadeIn();
    });
    renamingEntryModal.on('hide.bs.modal', function (e) {
        bodyMask.fadeOut();
    });

    var initUploadingModal = function () {
        selectFileDiv.hide();
        uploadingProgressBar.css("width", "0%");
        uploadingSpeedDiv.html("0 B/s");
        writingProgressBar.css("width", "0%");
        writingSpeedDiv.html("0 B/s");
        uploadingProgressDiv.show();
        startUploadingButton.attr("disabled", true);
        startUploadingButton.html("<span class='spinner-border spinner-border-sm' role='status'></span>")
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
        startUploadingButton.html("Upload")
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
        constructor(file, path, fileName, threadCount) {
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
                    showAlertMessage(data, "Error");
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
                        showAlertMessage("Uploading task canceled!", "Info");
                    } else {
                        showAlertMessage(data, "Error");
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
                    notifyUploadingResult(thisObj.uploadingProgress, uploadingSpeed, thisObj.writingProgress, writingSpeed);
                    if(status===0){
                        thisObj._checkUploadingProgress(thisObj, timeStamp, uploadingSpeed, writingSpeed);
                    }else{
                        resetUploadingModal();
                        showAlertMessage("<u>" + thisObj.fileName + "</u> uploaded!", "Info");
                        refreshFileSystemEntryList();
                    }
                } else {
                    if(!thisObj.isCanceled && !thisObj.errorDetected){
                        thisObj.errorDetected = true;
                        showAlertMessage(data, "Error");
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
                showAlertMessage("Unknown error encountered while uploading Chunk #" + index + " of [" + thisObj.fileName + "]", "Error");
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
            fileUploader = new FileUploader(file, sessionStorage.getItem("path"), fileNameInput.val(), uploadingThreadCount);
            fileUploader.startUploading();
        } else {
            showAlertMessage("Please select a file!", "Warning");
        }
    });
    cancelUploadingButton.click(function () {
        if (fileUploader !== undefined) {
            fileUploader.cancelUploading();
        } else {
            resetUploadingModal();
        }
    });
    createFolderButton.click(function(){
        if (folderNameInput.val() !== undefined && folderNameInput.val() !== null && folderNameInput.val() !== "") {
            var folderInfo = sessionStorage.getItem("path") + "|" + folderNameInput.val();
            $.post("/File/CreateFolder", folderInfo, function (data) {
                if (data.slice(0, 1) !== "#") {
                    showAlertMessage("<u>"+folderNameInput.val()+"</u> created!", "Info");
                    creatingFolderModal.modal("hide");
                    refreshFileSystemEntryList();
                } else {
                    showAlertMessage(data, "Error");
                }
            });
        } else {
            showAlertMessage("Please input the folder name!", "Warning");
        }
    });
    cancelCreatingButton.click(function(){
        creatingFolderModal.modal("hide");
    });
    creatingFolderModal.on('hide.bs.modal', function (e) {
        folderNameInput.val("");
    });
    deleteButton.click(function () {
        showConfirmationDialog("Delete File?", "<u class='text-wrap text-break'>" + fileModalLabel.text()+ "</u> will be deleted.", function(){
            var fullPath = sessionStorage.getItem("path") + "/" + fileModalLabel.text();
            $.post("/File/DeleteFile", fullPath, function (data) {
                if (data.slice(0, 1) !== "#") {
                    showAlertMessage("<u>"+fileModalLabel.text()+"</u> deleted!", "Info");
                    fileModal.modal("hide");
                    refreshFileSystemEntryList();
                } else {
                    showAlertMessage(data, "Error");
                }
            });
        });
    });
    downloadButton.click(function () {
        showConfirmationDialog("Download File?", "<u class='text-wrap text-break'>" + fileModalLabel.text() + "</u> will be downloaded.", function(){
            var fullPath = sessionStorage.getItem("path") + "/" + fileModalLabel.text();
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
    });
    fileInput.change(function () {
        if (fileInput[0].files.length > 0) {
            fileNameInput.val(fileInput[0].files[0].name);
        } else {
            fileNameInput.val("");
        }
    });
    var tooltipDeletingOptionClickCallback = function(){
        optionMenuMask.hide();
        selectedEntry.tooltip("hide");
        body.css("overflow-y", "");
        var type = "Folder";
        if (selectedEntry.hasClass("entryTableFile")){
            type = "File";
        }
        showConfirmationDialog("Delete "+type+"?", "<u class='text-wrap text-break'>" + selectedEntry.attr("entryName") + "</u> will be deleted.", function(){
            var fullPath = sessionStorage.getItem("path") + "/" + selectedEntry.attr("entryName");
            $.post("/File/Delete"+type, fullPath, function (data) {
                if (data.slice(0, 1) !== "#") {
                    showAlertMessage("<u>"+selectedEntry.attr("entryName")+"</u> deleted!", "Info");
                    refreshFileSystemEntryList();
                } else {
                    showAlertMessage(data, "Error");
                }
            });
        });
    };
    var tooltipPropertyOptionClickCallback = function(){
        optionMenuMask.hide();
        selectedEntry.tooltip("hide");
        body.css("overflow-y", "");
        var type = "Folder";
        if (selectedEntry.hasClass("entryTableFile")){
            type = "File";
        }
        nameInPropForm.html(selectedEntry.attr("entryName"));
        pathInPropForm.html(sessionStorage.getItem("path").substring(1)+"/"+selectedEntry.attr("entryName"));
        urlInPropForm.html(window.location.href+"UrlAccess/"+encodeURIComponent(sessionStorage.getItem("path")+"/"+selectedEntry.attr("entryName")));
        lastModifiedTimeInPropForm.html(selectedEntry.attr("lastModifiedTime"));
        typeInPropForm.html(type);
        sizeInPropForm.html(selectedEntry.attr("size"));
        propertyModal.modal("show");
    };
    var tooltipRenamingOptionClickCallback = function(){
        optionMenuMask.hide();
        selectedEntry.tooltip("hide");
        body.css("overflow-y", "");
        newEntryNameInput.val(selectedEntry.attr("entryName"));
        renamingEntryModal.modal("show");
    };
    renameEntryButton.click(function(){
        if (newEntryNameInput.val() !== undefined && newEntryNameInput.val() !== null && newEntryNameInput.val() !== "" && newEntryNameInput.val() !== selectedEntry.attr("entryName")) {
            var renamingInfo = sessionStorage.getItem("path") +"|"+selectedEntry.attr("entryName")+ "|" + newEntryNameInput.val();
            $.post("/File/RenameEntry", renamingInfo, function (data) {
                if (data.slice(0, 1) !== "#") {
                    showAlertMessage("<u>" + selectedEntry.attr("entryName") + "</u> renamed as <u>" + newEntryNameInput.val() + "</u>!", "Info");
                    renamingEntryModal.modal("hide");
                    refreshFileSystemEntryList();
                } else {
                    showAlertMessage(data, "Error");
                }
            });
        } else {
            showAlertMessage("Please input a new name!", "Warning");
        }
    });
    var calcSizeToDisplay = function (size) {
        var unit = 'B';
        var fileSizeDisplay = size;
        if (fileSizeDisplay / 1024 > 1) {
            fileSizeDisplay = fileSizeDisplay / 1024;
            unit = 'KB';
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
        }
        return fileSizeDisplay.toFixed(2) + " " + unit;
    };

    var getFileSystemEntry = function (path) {
        sessionStorage.setItem("path", path);
        var pathNodeArray = path.split("/");
        if (path !==""){
            currentFolderNameElement.html(pathNodeArray[pathNodeArray.length-1]);
        } else {
            currentFolderNameElement.html(serviceNodeName);
        }
        pathDropdownMenu.html("<span class='dropdown-item text-truncate pathDropdownMenuItem' style='padding-left:1rem;'><i class='fas fa-server' style='color:rgb(150,150,150);margin-right:.5rem;'></i>" + serviceNodeName + "</span>")
        if (pathNodeArray.length>=2){
            pathDropdownMenu.append("<span class='dropdown-item text-truncate pathDropdownMenuItem' style='padding-left:1.5rem;'><i class='fas fa-hdd' style='color:rgb(150,150,150);margin-right:.5rem;'></i>" + pathNodeArray[1] + "</span>");
            for(var index = 2; index < pathNodeArray.length; index++) {
                pathDropdownMenu.append("<span class='dropdown-item text-truncate pathDropdownMenuItem' style='padding-left:" + (1 + 0.5*index) + "rem;'><i class='far fa-folder' style='color:rgb(150,150,150);margin-right:.5rem;'></i>" + pathNodeArray[index] + "</span>");
            }
        }
        $("span.pathDropdownMenuItem").click(function () {
            var length = $("span.pathDropdownMenuItem").index(this) + 1;
            var targetPath = "";
            for (var index = 1; index < length; index++) {
                targetPath += "/" + $("span.pathDropdownMenuItem").eq(index).text();
            }
            getFileSystemEntry(targetPath);
        });
        folderNumberElement.html('...');
        fileNumberElement.html('...');
        fileListTableBody.empty();
        fileListSpinner.show();
        $.post("/File/GetFileSystemEntries", path, function (data) {
            if (data.slice(0,1)!=="#") {
                fileListSpinner.hide();
                var fileSystemEntries = data.split("|");
                filenameList = [];
                var folderCount = 0;
                for (var index = 0; index < fileSystemEntries.length - 1; index++) {
                    var entryInfo = fileSystemEntries[index].split("*");
                    var entryName = entryInfo[0];
                    if (path === "") {
                        var usedSpace = entryInfo[1];
                        var totalSpace = entryInfo[2];
                        var usedPercent = 100*usedSpace/totalSpace;
                        var progressbarType = "bg-info";
                        if (usedPercent>=90){
                            progressbarType = "bg-danger";
                        }
                        $("<tr class='entryTableFolder' entryName=\"" + entryName + "\" size='-' lastModifiedTime='-'>" +
                            "<td class='px-0 py-1' style='position:relative;'><div class='card d-inline-block mr-2 entryIconCard'><div class='card-body p-2 entryIconCardBody'><i class='HddIcon fas fa-hdd' aria-hidden='true'></i></div></div>" +
                            "<div class='d-inline-block verticalCenter pb-1' style='width:calc(100% - 3rem);'><div class='text-truncate' style='width:100%;margin-bottom:.125rem;position:relative;'>" + entryName +
                            "<div style='font-size:.7rem;color:rgb(150,150,150);position:absolute;bottom:0;right:0;'>" + calcSizeToDisplay(usedSpace) + " / " + calcSizeToDisplay(totalSpace) + "</div></div><div class='progress' style='height:.5rem;'>" +
                            "<div class='progress-bar "+progressbarType+"' role='progressbar' style='width: "+usedPercent+"%;' aria-valuenow='"+usedPercent+"' aria-valuemin='0' aria-valuemax='100'></div></div></div></td></tr>").appendTo(fileListTableBody).hide().fadeIn();
                    } else {
                        var lastModifiedTime = entryInfo[1];
                        var size = entryInfo[2];
                        var tooltipContent =
                            "<div class=\"row tooltipMenu m-0\">" +
                                "<div class=\"col-4 border-right d-flex tooltipOptionLeft tooltipDeletingOption tooltipOption justify-content-center\">" +
                                    "<i class=\"fas fa-trash-alt\"></i>" +
                                "</div>" +
                                "<div class=\"col-4 border-right d-flex tooltipRenamingOption tooltipOption justify-content-center\">" +
                                    "<i class=\"fas fa-highlighter\"></i>" +
                                "</div>" +
                                "<div class=\"col-4 d-flex tooltipOptionRight tooltipPropertyOption tooltipOption justify-content-center\">" +
                                    "<i class=\"fas fa-eye\"></i>" +
                                "</div>" +
                            "</div>";
                        if (size === "") {
                            $("<tr class='entryTableItem entryTableFolder' entryName=\"" + entryName + "\" size='-' lastModifiedTime='" + lastModifiedTime + "' data-toggle='tooltip' data-trigger='manual' data-placement='auto' data-html='true' title='" + tooltipContent + "'>" +
                                "<td class='px-0 py-1' style='position:relative;'><div class='card d-inline-block mr-2 entryIconCard'><div class='card-body p-2 entryIconCardBody'><i class='folderIcon fas fa-folder' aria-hidden='true'></i></div></div>" +
                                "<div class='d-inline-block verticalCenter pb-1' style='width:calc(100% - 3rem);'><div class='text-truncate' style='width:100%;margin-bottom:.125rem;'>" + entryName + "</div><div style='font-size:.7rem;color:rgb(150,150,150);line-height:100%;'>" +
                                lastModifiedTime + "</div></div></td></tr>").appendTo(fileListTableBody).hide().fadeIn();
                            folderCount++;
                        } else {
                            var fileSizeDisplay = calcSizeToDisplay(parseFloat(size));
                            $("<tr class='entryTableItem entryTableFile' index='" + filenameList.length + "' size='" + size + " B (about " + fileSizeDisplay + ")' lastModifiedTime='" + lastModifiedTime + "' entryName=\"" + entryName + "\" data-toggle='tooltip' data-trigger='manual' data-placement='auto' data-html='true' title='" + tooltipContent + "'>" +
                                "<td class='px-0 py-1' style='position:relative;'><div class='card d-inline-block mr-2 entryIconCard'><div class='card-body p-2 entryIconCardBody'><i class='fileIcon fas fa-file-alt' aria-hidden='true'></i></div></div>" +
                                "<div class='d-inline-block verticalCenter pb-1' style='width:calc(100% - 3rem);p'><div class='text-truncate' style='width:100%;margin-bottom:.125rem;'>" + entryName + "</div><div style='font-size:.7rem;color:rgb(150,150,150);line-height:100%;'>" +
                                lastModifiedTime + "<div class='float-right'>" + fileSizeDisplay + "</div></div></div></td></tr>").appendTo(fileListTableBody).hide().fadeIn();
                            filenameList.push(entryInfo[0]);
                        }
                    }
                }
                folderNumberElement.text(folderCount);
                fileNumberElement.text(filenameList.length);
                $("tr.entryTableItem").contextmenu(function(event){
                    selectedEntry = $(this);
                    selectedEntry.tooltip("show");
                    body.css("overflow-y", "hidden");
                    optionMenuMask.show();
                    event.preventDefault();
                    $("div.tooltipDeletingOption").click(tooltipDeletingOptionClickCallback);
                    $("div.tooltipRenamingOption").click(tooltipRenamingOptionClickCallback);
                    $("div.tooltipPropertyOption").click(tooltipPropertyOptionClickCallback);
                });
                $(function () {
                    $("[data-toggle='tooltip']").tooltip();
                });

                $("tr.entryTableFolder").click(function () {
                    folderName = $(this).attr("entryName");
                    getFileSystemEntry(sessionStorage.getItem("path") + "/" + folderName);
                });

                $("tr.entryTableFile").click(function () {
                    currentFileIndex = Number($(this).attr("index"));
                    showFileInfo($(this).attr("entryName"));
                });
            } else {
                showAlertMessage(data, "Error");
            }

        });
    };
    var refreshFileSystemEntryList = function () {
        getFileSystemEntry(sessionStorage.getItem("path"));
    };
    var showFileInfo = function (fileName) {
        fileModalLabel.text(fileName);
        if (currentFileIndex===0){
            previousFileButton.addClass("disabled");
        }else{
            previousFileButton.removeClass("disabled");
        }
        if(currentFileIndex===filenameList.length-1){
            nextFileButton.addClass("disabled");
        }else{
            nextFileButton.removeClass("disabled");
        }
        var fileNameArray = fileName.split(".");
        var extendName = fileNameArray[fileNameArray.length - 1];
        if (extendName === "mp4" || extendName === "MP4") {
            fileModalBody.html("<video id='fileModalVideo' width='100%' height='auto'controls preload='none'>" +
                "<source src=\"/Video/PlayVideo/" + encodeURIComponent(sessionStorage.getItem("path") + "/" + fileName) + "\" type='video/mp4'></video>");
        } else if (extendName === "JPG" || extendName === "PNG" || extendName === "GIF" ||
            extendName === "jpg" || extendName === "png" || extendName === "gif") {
            fileModalBody.html("<img src=\"/Image/DisplayImage/" + encodeURIComponent(sessionStorage.getItem("path") + "/" + fileName) + "\" class='img-thumbnail' style='width:100%;'>");
        } else {
            fileModalBody.html("<div class='fileInfo' style='font-size:90px;color:rgb(130, 130, 130);'>" +
                "<i class='fas fa-file-alt' aria-hidden='true'></i></div>");
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

    optionMenuMask.click(function(){
        selectedEntry.tooltip("hide");
        body.css("overflow-y", "");
        optionMenuMask.hide();
    });

    var toggleFloatingMenu = function(){
        if(toggleButtonIcon.hasClass("toggleButtonIconExpand")){
            toggleButtonIcon.removeClass("toggleButtonIconExpand");
            creatingButton.removeClass("creatingButtonExpand");
            uploadingButton.removeClass("uploadingButtonExpand");
            settingButton.removeClass("settingButtonExpand");
            floatingMenuMask.hide();
            floatingMenuContainer.css("z-index", "");
        }else {
            toggleButtonIcon.addClass("toggleButtonIconExpand");
            creatingButton.addClass("creatingButtonExpand");
            uploadingButton.addClass("uploadingButtonExpand");
            settingButton.addClass("settingButtonExpand");
            floatingMenuMask.show();
            floatingMenuContainer.css("z-index", "1000");
        }
    };

    floatingMenuMask.click(function(){
        toggleFloatingMenu();
    });

    toggleButton.click(function(){
        toggleFloatingMenu();
    });

    creatingButton.click(function(){
        if(sessionStorage.getItem("path")===""){
            showAlertMessage("Cannot create folders outside disks!", "Warning");
        }else{
            toggleFloatingMenu();
            creatingFolderModal.modal("show");
        }
    });

    uploadingButton.click(function(){
        if(sessionStorage.getItem("path")===""){
            showAlertMessage("Cannot upload files outside disks!", "Warning");
        }else{
            toggleFloatingMenu();
            uploadingModal.modal("show");
        }
    });

    settingButton.click(function(){
        toggleFloatingMenu();
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
    setMaxWidthForNav();
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