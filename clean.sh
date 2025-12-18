rm -rf builds/
rm -rf downloads/
rm -rf servers/

git stash push -m "temp"
git pull --rebase