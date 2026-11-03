"""One public-data probe per CI build; failure is recorded, never bypassed."""
import json, urllib.request, urllib.error
from pathlib import Path
report={}
for endpoint in ('servers','events'):
    url='https://api.holyworld.me/v1/'+endpoint
    try:
        with urllib.request.urlopen(urllib.request.Request(url,headers={'Accept':'application/json','User-Agent':'RIMVISUALS/0.4.2-helpers.2'}),timeout=12) as r:
            body=r.read(262145)
            if len(body)>262144: raise ValueError('Body exceeds 256 KiB')
            data=json.loads(body)
            report[endpoint]={'http':r.status,'root_type':type(data).__name__,'entries':len(data) if isinstance(data,(list,dict)) else None}
            Path('hw-api-'+endpoint+'.json').write_text(json.dumps(data,ensure_ascii=False),encoding='utf-8')
    except urllib.error.HTTPError as e: report[endpoint]={'http':e.code,'error':'Unavailable; no bypass or fabricated fallback'}
    except Exception as e: report[endpoint]={'error':type(e).__name__}
Path('hw-api-probe.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
print(json.dumps(report))
