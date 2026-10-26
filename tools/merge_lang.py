import json,glob,os
L='src/main/resources/assets/hukhong/lang/'
vi=json.load(open(L+'vi_vn.json',encoding='utf-8'));en=json.load(open(L+'en_us.json',encoding='utf-8'))
n=0
for f in glob.glob('docs/research/lang-*.txt'):
  for line in open(f,encoding='utf-8'):
    p=[x.strip() for x in line.split('|')]
    if len(p)<3 or not p[1].startswith('vi:'):continue
    vi.setdefault(p[0],p[1][3:].strip());en.setdefault(p[0],p[2][3:].strip());n+=1
  os.remove(f)
for d,fn in((vi,'vi_vn'),(en,'en_us')):json.dump(d,open(L+fn+'.json','w',encoding='utf-8'),ensure_ascii=False,indent=2)
print(n)
