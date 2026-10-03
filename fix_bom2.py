import os
path = 'backend/src/test/java/com/aicodereview/service/LLMSecretLeakTest.java'
with open(path, 'rb') as f:
    content = f.read()
if content.startswith(b'\xef\xbb\xbf'):
    with open(path, 'wb') as f:
        f.write(content[3:])
