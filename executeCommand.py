import requests

# The secret is generated on first start and written to main-config.yml as web.command-secret.
# The launcher prints it once when it creates it.
secret = input("Secret: ")
server = input("Server: ")
command = input("Command: ")
json = {
    "command": command,
    "secret": secret,
    "server": server
}
print(requests.post("http://localhost:8080/command", json=json))
