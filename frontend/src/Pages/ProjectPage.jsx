import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import '../styles/ProjectPage.css';
import { useTheme } from '../Components/Theme';
import MonacoEditor from '@monaco-editor/react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import global from 'global';

const ProjectPage = () => {
    const { slug } = useParams();
    const navigate = useNavigate();
    const { isDarkMode } = useTheme();
    const [project, setProject] = useState(null);
    const [projectStructure, setProjectStructure] = useState([]);
    const [selectedFile, setSelectedFile] = useState(null);
    const [fileContent, setFileContent] = useState('');
    const [output, setOutput] = useState('');
    const [showCreatePopUP, setShowPopup] = useState(false);
    const [newFilePath, setNewFilePath] = useState('');
    const [newFileType, setNewFileType] = useState('file');
    const [showDeletePopup, setShowDeletePopup] = useState(false);
    const [fileToDelete, setFileToDelete] = useState('');
    const [showRenamePopup, setShowRenamePopup] = useState(false);
    const [fileToRename, setFileToRename] = useState('');
    const [newFileName, setNewFileName] = useState('');
    const [code, setCode] = useState(''); 
    const [stompClient, setStompClient] = useState(null);
    const stompClientRef = useRef(null);
    const [userRole, setUserRole] = useState('');

    const FileExplorer = ({ files, onFileSelect }) => (
        <div className = "file-container">
            <ul className="file-list">
            {files.map((file) => {
                // Create a Date object from the last modified string
                const lastModifiedDate = new Date(file.lastModefied);
                // Format the date to a more readable format
                const formattedDate = lastModifiedDate.toLocaleString('en-US', {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit',
                    hour12: true,
                });
                return (
                        <li
                            key={file.fileId}
                            onClick={() => onFileSelect(file.fileName, file.filePath)}
                            className={`file-item ${selectedFile === file.fileName ? 'active' : ''}`}
                        >
                            {file.fileName}{" "}<small><small><small>{formattedDate}</small></small></small>
                        </li>
                );
            })}
            </ul>
        </div>
    );

    useEffect(() => {
        const fetchProjectData = async () => {
            try {
                // Fetch project details
                const projectResponse = await fetch(`http://localhost:8080/dashboard/project/get?projectSlug=${slug}`, {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
                    },
                });
    
                if (!projectResponse.ok) throw new Error('Failed to fetch project details');
    
                const projectData = await projectResponse.json();
                setProject(projectData);
                localStorage.setItem('projectInfo', JSON.stringify(projectData));
    
                // Fetch project structure after project details
                const structureResponse = await fetch(`http://localhost:8080/dashboard/project/structure?projectSlug=${slug}`, {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
                    },
                });
    
                if (!structureResponse.ok) throw new Error('Failed to fetch project structure');
    
                const structureData = await structureResponse.json();
                setProjectStructure(structureData);
    
                // Fetch user role
                const userEmail = localStorage.getItem('email');
                const roleResponse = await fetch(`http://localhost:8080/dashboard/project/role/userRole?projectSlug=${slug}&userEmail=${userEmail}`, {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
                    },
                });
    
                if (!roleResponse.ok) throw new Error('Failed to fetch user role');
    
                const roleData = await roleResponse.json();
                setUserRole(roleData.role);
                console.log('User role:', roleData.role);
    
            } catch (err) {
                console.error('Error fetching project data:', err.message);
            }
        };
    
        const connectWebSocket = () => {
            const token = localStorage.getItem('authToken');
            const socket = new SockJS(`http://localhost:8080/ws?token=${token}`);
            const stompClient = Stomp.over(socket);
    
            stompClient.connect(
                {},
                (frame) => {
                    console.log('Connected to WebSocket:', frame);
                    
                    stompClient.subscribe(`/topic/editor/${slug}`, (message) => {
                        const updatedProject = JSON.parse(message.body);
                        console.log('Received updated project:', updatedProject);
                        
                        if (updatedProject && updatedProject.code) {
                            if (updatedProject.file === selectedFile) {
                                setFileContent(updatedProject.code);
                            } else {
                                console.log('No update: The file is different from the selected file.');
                            }
                        }
                    });
    
                    stompClient.subscribe(`/topic/status/${slug}`, (message) => {
                        const executionResult = JSON.parse(message.body);
                        console.log('Received execution result:', executionResult);
    
                        if (executionResult.status === 'COMPLETED') {
                            setOutput(executionResult.output);
                        } else if (executionResult.status === 'ERROR') {
                            setOutput(`Execution Failed: ${executionResult.output}`);
                        }
                    });
    
                    stompClient.subscribe(`/topic/change/${slug}`, (message) => {
                        const changeResult = JSON.parse(message.body);
    
                        if (changeResult.status === 'COMPLETED') {
                            fetchProjectStructure();
                        } else if (changeResult.status === 'ERROR') {
                            console.log("File operation failed");
                        }
                    });
                },
                (error) => {
                    console.error('WebSocket error:', error);
                }
            );
    
            stompClientRef.current = stompClient; // Set the ref to the stompClient
        };
    
        fetchProjectData().then(() => {
            const stompClient = connectWebSocket();
    
            // Cleanup function
            return () => {
                localStorage.removeItem('projectInfo');
                if (stompClientRef.current) {
                    stompClientRef.current.disconnect(() => {
                        console.log('WebSocket disconnected');
                    });
                }
            };
        });
    }, [selectedFile, slug]);
    
    
    const handleCodeChange = (newValue) => {
        // Update the file content with the new value from the editor
        setFileContent(newValue);

        if (stompClientRef.current) {
            const message = {
                code: newValue,
                user: localStorage.getItem("email"),
                timestamp: Date.now(),
                file: selectedFile,
            };

            // Send the message over the WebSocket
            stompClientRef.current.send(`/app/edit/${slug}`, {}, JSON.stringify(message));
        } else {
            console.log('stompClient is not connected yet');
        }
    };

    const handleSettings = () => {
        navigate(`/projects/${slug}/settings`);
    };

    const handleFileCreate = () => {
        setShowPopup(true);
    };

    const handleRenameFile = () => {
        setShowRenamePopup(true);
    };

    const handleDeleteFile = () => {
        setShowDeletePopup(true);
    };

    const isButtonDisabled = (requiredRoles) => {
        return !requiredRoles.includes(userRole);
    };

const handleRenameSubmit = async () => {
    try {
        const requestBody = {
            fileName: fileToRename,
            filePath: "", 
            newFileName: newFileName,
            projectSlug: slug,
        };

        const response = await fetch(`http://localhost:8080/dashboard/project/files/rename`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
            },
            body: JSON.stringify(requestBody),
        });

        if (!response.ok) {
            const errorMessage = await response.text();
            throw new Error(`Failed to rename file: ${errorMessage}`);
        }


        setFileToRename(''); // Clear current file name
        setNewFileName(''); // Clear new file name
        setShowRenamePopup(false); // Hide popup
        fetchProjectStructure();
    } catch (error) {
        console.error(error.message);
        alert(error.message);
   }
};

    const handleDeleteSubmit = async () => {
        try {
            const requestBody = {
                fileName: fileToDelete,
                filePath: "",
                newFileName: "",
                projectSlug: slug,
            };

            const response = await fetch(`http://localhost:8080/dashboard/project/files/delete`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
                },
                body: JSON.stringify(requestBody),
            });

            if (!response.ok) {
                const errorMessage = await response.text();
                throw new Error(`Failed to rename file: ${errorMessage}`);
            }
            setFileToDelete(''); // Clear new file name
            setShowDeletePopup(false); // Hide popup
            fetchProjectStructure();
        } catch (error) {
            console.error(error.message);
            alert(error.message);
        }
    };

    const handleFileSelect = (fileName) => {
        fetch(`http://localhost:8080/dashboard/project/files/get-contents`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
            },
            body: JSON.stringify({ projectSlug: slug, fileName, newFileName: "", filePath: "" }),
        })
            .then((response) => {
                if (!response.ok) throw new Error('Failed to fetch file content');
                return response.text();
            })
            .then((content) => {
                setSelectedFile(fileName);
                setFileContent(content);
            })
            .catch((err) => console.error(err.message));
    };

    const handleSaveCode = () => {
        const formData = new FormData();
        const file = new File([fileContent], selectedFile, { type: "text/plain" });
        formData.append("file", file);
        formData.append("projectSlug", slug);
        formData.append("fileName", selectedFile);
        formData.append("newFileName", "");
        formData.append("filePath", "");

        fetch(`http://localhost:8080/dashboard/project/files/update-contents`, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${localStorage.getItem('authToken')}` },
            body: formData,
        })
            .then((response) => {
                if (!response.ok) throw new Error('Failed to update file contents');
                return response.json();
            })
            .then(() => console.log('File contents updated successfully'))
            .catch((err) => console.error(err.message));
    };

    const handleExecuteCode = async () => {
        const executeDto = {
            language: "python", 
            fileDto: {
                fileName: selectedFile,
                filePath: "",
                newFileName: "",
                projectSlug: slug,
            }
        };
    
        try {
            const response = await fetch('http://localhost:8080/dashboard/project/code/execute', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
                },
                body: JSON.stringify(executeDto),
            });
    
            if (response.ok) {
                const result = await response.json();
                setOutput(`Code executed successfully: ${result.result}`);
            } else {
                const errorResult = await response.json();
                setOutput(`Execution failed: ${errorResult.message}`);
            }
        } catch (error) {
            setOutput(`Error occurred: ${error.message}`);
        }
    };     

    const handleClearOutput = () => {
        setOutput('');
    };

    const handleCreateSubmit = () => {
        const pathParts = newFilePath.split('/');
        const fileNameOrFolderName = pathParts.pop();
        const folderPath = pathParts.length ? pathParts.join('/') + '/' : '';
        const requestUrl = 'http://localhost:8080/dashboard/project/files/upload';
    
        const formData = new FormData();
        const file = new File([], fileNameOrFolderName);
        formData.append('file', file);
        formData.append('projectSlug', slug);
        formData.append('filePath', folderPath);
        formData.append('fileName', fileNameOrFolderName);
    
        fetch(requestUrl, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${localStorage.getItem('authToken')}` },
            body: formData,
        })
        .then((response) => {
            if (!response.ok) throw new Error('Failed to create file');
    
            // Check if the response is in JSON format
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return response.json(); // Parse as JSON
            } else {
                return response.text(); // Handle plain text response
            }
        })
        .then((data) => {
            if (typeof data === 'string') {
                console.log(data); // Handle the plain text response
            } else {
                console.log('File created successfully'); // Handle JSON response if any
            }
            
            setNewFilePath(''); // Clear the input field
            setShowPopup(false); // Hide the popup
            fetchProjectStructure(); // Fetch the updated project structure
        })
        .catch((err) => console.error(err.message));
    };

    const fetchProjectStructure = () => {
        fetch(`http://localhost:8080/dashboard/project/structure?projectSlug=${slug}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
            },
        })
        .then((response) => {
            if (!response.ok) throw new Error('Failed to fetch project structure');
            return response.json();
        })
        .then((structure) => {
            console.log('Project structure fetched successfully', structure);
            setProjectStructure(structure);
        })
        .catch((err) => console.error(err.message));
    };

    return (
        <div className={`project-page-container ${isDarkMode ? 'dark-mode' : 'light-mode'}`}>
            <div className="project-header">
                <h1>{project ? project.name : 'Loading project...'}</h1>
                <button className="settings-btn roleCustom" onClick={handleSettings} disabled={isButtonDisabled(['owner'])}>Developers</button>
            </div>
            <div className="columns-container">
                <div className="column file-explorer">
                    <h2>File Explorer</h2>
                    <button className='roleCustom' onClick={handleFileCreate} disabled={isButtonDisabled(['editor', 'owner'])}>New File</button>
{/*                     <button className='roleCustom' onClick={handleRenameFile} disabled={isButtonDisabled(['editor', 'owner'])}>Rename</button> */}
                    <button className='roleCustom' onClick={handleDeleteFile} disabled={isButtonDisabled(['editor', 'owner'])}>Delete</button>
                    {projectStructure.length > 0 ? (
                        <FileExplorer files={projectStructure} onFileSelect={handleFileSelect} />
                    ) : (
                        <p>No Files Found</p>
                    )}
                </div>

                <div className="column code-editor">
                    <h2>Code Editor</h2>
                    {selectedFile ? (
                        <div> 
                            <div className="codeEditor">
                                <MonacoEditor

                                height="400px"
                                language="python"
                                theme={isDarkMode ? 'vs-dark' : 'light'}
                                value={fileContent}
                                onChange={handleCodeChange}
                                options={{
                                    readOnly: isButtonDisabled(['editor', 'owner']), 
                                }}
                            /></div>
                          
                            <button className='roleCustom' onClick={handleSaveCode} disabled={isButtonDisabled(['editor', 'owner'])}>Save</button>
                            <button className='roleCustom' onClick={handleExecuteCode} disabled={isButtonDisabled(['editor', 'owner', 'viewer'])}>Execute</button>
                        </div>
                    ) : (
                        <p></p>
                    )}
                </div>
                {showCreatePopUP && (
                    <div className="popup-overlay">
                        <div className={`popup ${isDarkMode ? 'dark-mode' : 'light-mode'} show`}>
                            <h3>Create New File</h3>
                            <label htmlFor="filePath">Enter file name:</label>
                            <input
                                type="text"
                                id="filePath"
                                value={newFilePath}
                                onChange={(e) => setNewFilePath(e.target.value)}
                                placeholder="file.py"
                            />
                            <button onClick={handleCreateSubmit}>Create</button>
                            <button onClick={() => setShowPopup(false)}>Cancel</button>
                        </div>
                    </div>
                )}

                {showRenamePopup && (
                    <div className="popup-overlay">
                        <div className={`popup ${isDarkMode ? 'dark-mode' : 'light-mode'} show`}>
                            <h3>Rename File</h3>
                            <label htmlFor="filePath">Select current file:</label>
                            <select
                                value={fileToRename}
                                onChange={(e) => setFileToRename(e.target.value)}
                            >
                                <option value="" disabled>Select a file</option>
                                {projectStructure.map((file) => (
                                    <option key={file.fileId} value={file.fileName}>{file.fileName}</option>
                                ))}
                            </select>
                            <label htmlFor="newFileName">New file name:</label>
                            <input
                                type="text"
                                placeholder="New file name..."
                                value={newFileName}
                                onChange={(e) => setNewFileName(e.target.value)}
                            />
                            <button onClick={handleRenameSubmit}>Rename</button>
                            <button onClick={() => setShowRenamePopup(false)}>Cancel</button>
                        </div>
                    </div>
                )}


                {showDeletePopup && (
                    <div className="popup-overlay">
                        <div className={`popup ${isDarkMode ? 'dark-mode' : 'light-mode'} show`}>
                            <h3>Delete File</h3>
                            <label htmlFor="filePath">Select file to delete:</label>
                            <select
                                value={fileToDelete}
                                onChange={(e) => setFileToDelete(e.target.value)}
                            >
                                <option value="" disabled>Select a file</option>
                                {projectStructure.map((file) => (
                                    <option key={file.fileId} value={file.fileName}>{file.fileName}</option> // Assuming fileId is unique
                                ))}
                            </select>
                            <button onClick={handleDeleteSubmit}>Delete</button>
                            <button onClick={() => setShowDeletePopup(false)}>Cancel</button>
                        </div>
                    </div>
                )}

            </div>
            <div className="column output-console">
                <h2 style={{ color: '#fff' }}>Output Console</h2>
                <pre className='outputResult'>{output}</pre>
                <button onClick={handleClearOutput}>Clear Output</button>
            </div>
        </div>
    );
};

export default ProjectPage;
