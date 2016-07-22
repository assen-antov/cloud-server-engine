# cloud-server-engine
A file server implemented as Java servlet. In response to HTTP POST and GET requests returns directory listings, file data, etc. as JSON.   
Supports the following commands:
list_contents returns the contents of a directory
list_dirs returns the sub-directories in a directory
list_files returns the files in a directory
file_info returns information about a particular file
ping  tests if there is connection and keeps the session alive
download  initiates a file download
upload  initiates a file upload
recycle_file  moves a file to the bin
recycle_dir moves a directory to the bin
login user log in
logout user log out
change_pass changes the password of a user
user_info returns detailed information regarding user's profile
server_info returns detailed information regarding the server
settings_get  returns the value of a server setting
settings_set  changes the value of a server setting
list_settings returns all server settings
list_users  returns a list of all users
