# Script para eliminar emojis de archivos markdown
$mdFiles = Get-ChildItem -Path . -Filter "*.md" -Recurse

foreach ($file in $mdFiles) {
    Write-Host "Procesando: $($file.Name)" -ForegroundColor Cyan
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    
    # Lista de emojis a eliminar
    $emojis = @(
        '🔧', '✅', '❌', '🔍', '🧪', '📸', '📋', '⚠️', '📊', '🎯',
        '⭐', '💡', '📝', '🚀', '🛠️', '🔐', '🔑', '🎨', '🧩', '🗂️',
        '📦', '📂', '⚙️', '🖥️', '☑'
    )
    
    $newContent = $content
    foreach ($emoji in $emojis) {
        $newContent = $newContent -replace [regex]::Escape($emoji), ''
    }
    
    # Limpiar espacios dobles resultantes
    $newContent = $newContent -replace '  +', ' '
    $newContent = $newContent -replace '# +', '# '
    $newContent = $newContent -replace '## +', '## '
    $newContent = $newContent -replace '### +', '### '
    
    if ($content -ne $newContent) {
        Set-Content -Path $file.FullName -Value $newContent -Encoding UTF8 -NoNewline
        Write-Host "  -> Emojis eliminados" -ForegroundColor Green
    } else {
        Write-Host "  -> Sin cambios" -ForegroundColor Yellow
    }
}

Write-Host "`nProceso completado!" -ForegroundColor Green
