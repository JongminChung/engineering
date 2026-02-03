{{/*템플릿 함수를 정의하는 파일명으로는 _helpers.tpl을 사용하는 것이 일반적이지만 사실 _로 시작하기만 하면 된다.*/}}

{{- define "pacman.selectorLabels" -}} {{/*statement 이름을 정의한다.*/}}
app.kubernetes.io/name: {{ .Chart.Name}} {{/*해당 statement가 하는 일을 정의한다.*/}}
app.kubernetes.io/version: {{ .Chart.AppVersion }}
{{- end }}
