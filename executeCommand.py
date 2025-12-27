import requests
server = input("Server: ")
command = input("Command: ")
json = {
    "command": command,
    "secret": "67",
    "server": server
}
print(requests.post("http://localhost:8080/command", json=json))