package com.enlacedigital.CoordiApp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.enlacedigital.CoordiApp.Registrar.RegistrandoFragment1
import com.enlacedigital.CoordiApp.Registrar.RegistrandoFragment2
import com.enlacedigital.CoordiApp.Registrar.RegistrandoFragment3
import com.enlacedigital.CoordiApp.Registrar.RegistrandoFragment4
import com.enlacedigital.CoordiApp.Registrar.RegistrandoFragment5
import com.enlacedigital.CoordiApp.Registrar.RegistrandoFragment6
import com.enlacedigital.CoordiApp.Registrar.RegistrandoFragment7
import com.enlacedigital.CoordiApp.models.Option
import com.enlacedigital.CoordiApp.singleton.ApiServiceHelper
import com.enlacedigital.CoordiApp.singleton.PreferencesHelper
import com.enlacedigital.CoordiApp.utils.startNewActivity

class MenuRegistrando : Fragment() {
    val preferencesManager = PreferencesHelper.getPreferencesManager()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_menu_registrando, container, false)

        val buttonPaso1 = view.findViewById<Button>(R.id.buttonPaso1)
        val buttonPaso2 = view.findViewById<Button>(R.id.buttonPaso2)
        val buttonPaso3 = view.findViewById<Button>(R.id.buttonPaso3)
        val buttonPaso4 = view.findViewById<Button>(R.id.buttonPaso4)
        val buttonPaso5 = view.findViewById<Button>(R.id.buttonPaso5)
        val buttonPaso6 = view.findViewById<Button>(R.id.buttonPaso6)
        val buttonPaso7 = view.findViewById<Button>(R.id.buttonPaso7)

        val boton1= preferencesManager.getString("boton1")
        val boton2 = preferencesManager.getString("boton2")
        val boton3= preferencesManager.getString("boton3")
        val boton4 = preferencesManager.getString("boton4")
        val boton5= preferencesManager.getString("boton5")
        val boton6 = preferencesManager.getString("boton6")
        val boton7= preferencesManager.getString("boton7")

        if (boton1 == "listo1") {
            buttonPaso1.setBackgroundColor(Color.rgb(34,139,34))
            buttonPaso1.isEnabled = false
        }

        if (boton2 == "listo2") {
            buttonPaso2.setBackgroundColor(Color.rgb(34,139,34))
            buttonPaso2.isEnabled = false
        }

        if (boton3 == "listo3") {
            buttonPaso3.setBackgroundColor(Color.rgb(34,139,34))
            buttonPaso3.isEnabled = false
        }

        if (boton4 == "listo4") {
            buttonPaso4.setBackgroundColor(Color.rgb(34,139,34))
            buttonPaso4.isEnabled = false
        }

        if (boton5 == "listo5") {
            buttonPaso5.setBackgroundColor(Color.rgb(34,139,34))
            buttonPaso5.isEnabled = false
        }

        if (boton6 == "listo6") {
            buttonPaso6.setBackgroundColor(Color.rgb(34,139,34))
            buttonPaso6.isEnabled = false
        }

        if (boton7 == "listo7") {
            buttonPaso7.setBackgroundColor(Color.rgb(34,139,34))
            buttonPaso7.isEnabled = false
        }

        buttonPaso1.setOnClickListener {
            (activity as? Registrando)?.goToNextStep(1)
        }
        buttonPaso2.setOnClickListener {
            (activity as? Registrando)?.goToNextStep(2)
        }
        buttonPaso3.setOnClickListener {
            (activity as? Registrando)?.goToNextStep(3)
        }
        buttonPaso4.setOnClickListener {
            (activity as? Registrando)?.goToNextStep(4)
        }
        buttonPaso5.setOnClickListener {
            (activity as? Registrando)?.goToNextStep(5)
        }
        buttonPaso6.setOnClickListener {
            (activity as? Registrando)?.goToNextStep(6)
        }
        buttonPaso7.setOnClickListener {
            (activity as? Registrando)?.goToNextStep(7)
        }

        if(boton1 == "listo1" && boton2 == "listo2" && boton3 == "listo3" && boton4 == "listo4" && boton5 == "listo5" && boton6 == "listo6" && boton7 == "listo7")
        {
            (activity as? Registrando)?.intentFinal()
        }

        return view
    }
}
