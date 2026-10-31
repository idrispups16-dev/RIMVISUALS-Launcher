const fs=require('node:fs'),path=require('node:path');
// Containment is lexical AND rejects existing symlinks/junctions below the owned root.
// Does not claim protection against a malicious process racing filesystem operations as the same user.
function ownedPath(root,file){const base=path.resolve(root),target=path.resolve(file),relative=path.relative(base,target);if(path.isAbsolute(relative)||relative.split(path.sep).includes('..'))throw Error('Unsafe installation path');let current=base;for(const part of ['',...relative.split(path.sep).filter(Boolean)]){if(part)current=path.join(current,part);try{if(fs.lstatSync(current).isSymbolicLink())throw Error('Symbolic link/junction in managed path');}catch(e){if(e.code!=='ENOENT')throw e;}}return target;}
module.exports={ownedPath};
