#!/bin/bash
layout_dir="app/src/main/res/layout"
fixed_count=0

# List of new untracked files that need fixing
new_files=(
    "dialog_acceso_resultado.xml"
    "dialog_pago_efectivo.xml"
    "fragment_acceso_manual.xml"
    "fragment_apertura_caja.xml"
    "fragment_asistencia_detalle_sheet.xml"
    "fragment_asistencias_empleado.xml"
    "fragment_cambiar_plan.xml"
    "fragment_cancelar_membresia.xml"
    "fragment_carrito_bottom_sheet.xml"
    "fragment_checkout.xml"
    "fragment_cierre_caja.xml"
    "fragment_cliente_busqueda_sheet.xml"
    "fragment_configuracion_caja.xml"
    "fragment_configuracion_notificaciones.xml"
    "fragment_corte_caja.xml"
    "fragment_crear_editar_empleado.xml"
    "fragment_crear_editar_visita.xml"
    "fragment_empleado_detalle.xml"
    "fragment_empleados_list.xml"
    "fragment_historial_asistencias.xml"
    "fragment_historial_compras.xml"
    "fragment_historial_cortes.xml"
    "fragment_historial_membresias.xml"
    "fragment_horario_empleado.xml"
    "fragment_membresia_detalle.xml"
    "fragment_movimientos_caja.xml"
    "fragment_nomina_empleado.xml"
    "fragment_notificacion_detalle.xml"
    "fragment_notificaciones_list.xml"
    "fragment_nuevo_movimiento_sheet.xml"
    "fragment_pago_mixto.xml"
    "fragment_pago_tarjeta.xml"
    "fragment_permisos_empleado.xml"
    "fragment_planes_disponibles_sheet.xml"
    "fragment_qr_scanner.xml"
    "fragment_recepcion_dashboard.xml"
    "fragment_renovar_membresia.xml"
    "fragment_resumen_apertura.xml"
    "fragment_seguimiento_visita.xml"
    "fragment_ticket_compra.xml"
    "fragment_tienda.xml"
    "fragment_turnos_empleado.xml"
    "fragment_visita_detalle.xml"
    "fragment_visitas_list.xml"
)

for filename in "${new_files[@]}"; do
    filepath="$layout_dir/$filename"
    if [ ! -f "$filepath" ]; then
        continue
    fi
    
    content=$(cat "$filepath")
    original="$content"
    
    # Fix the specific pattern: extra </LinearLayout> before MaterialCardView close
    # Pattern: </LinearLayout>\n        </LinearLayout>\n        </MaterialCardView>
    # Use perl for multiline regex
    content=$(echo "$content" | perl -0777 -pe 's/(\s+)<\/LinearLayout>\n\s+<\/LinearLayout>\n(\s+)<\/com\.google\.android\.material\.card\.MaterialCardView>/\1<\/LinearLayout>\n\2<\/com.google.android.material.card.MaterialCardView>/g')
    
    # Also fix: </LinearLayout>\n    </LinearLayout>\n    </LinearLayout>\n        </MaterialCardView>
    content=$(echo "$content" | perl -0777 -pe 's/(\s+)<\/LinearLayout>\n\1<\/LinearLayout>\n\1<\/LinearLayout>\n(\s+)<\/com\.google\.android\.material\.card\.MaterialCardView>/\1<\/LinearLayout>\n\2<\/com.google.android.material.card.MaterialCardView>/g')
    
    # Fix duplicate android:orientation
    content=$(echo "$content" | sed -E 's/(android:orientation="[^"]*")\s+\1/\1/g')
    
    # Fix missing = in android:text
    content=$(echo "$content" | sed -E 's/android:text ([^="[:space:]]*)"/android:text="\1"/g')
    
    # Fix: ensure root LinearLayout is closed
    root_opens=$(echo "$content" | grep -c '^<LinearLayout')
    root_closes=$(echo "$content" | grep -c '^</LinearLayout>$')
    if [ "$root_opens" -gt "$root_closes" ]; then
        missing=$((root_opens - root_closes))
        for i in $(seq 1 $missing); do
            content="$content
</LinearLayout>"
        done
    fi
    
    if [ "$content" != "$original" ]; then
        echo "$content" > "$filepath"
        fixed_count=$((fixed_count + 1))
        echo "Fixed: $filename"
    fi
done

echo "Total fixed: $fixed_count"
