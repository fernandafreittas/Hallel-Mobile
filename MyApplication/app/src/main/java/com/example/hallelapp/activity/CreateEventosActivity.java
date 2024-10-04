package com.example.hallelapp.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hallelapp.R;
import com.example.hallelapp.htpp.HttpAdm;
import com.example.hallelapp.model.LocalEvento;
import com.example.hallelapp.model.LocalEventoLocalizacaoRequest;
import com.example.hallelapp.payload.requerimento.EventosRequest;
import com.example.hallelapp.payload.resposta.AllEventosListResponse;
import com.example.hallelapp.payload.resposta.AuthenticationResponse;
import com.example.hallelapp.recyclers.ColaboradorAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CreateEventosActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ColaboradorAdapter colaboradorAdapter;
    private AlertDialog loadingDialog;
    private Double valorDoEvento;
    private Double valorDescontoMembro;
    private Double valorDescontoAssociado;

    List<LocalEvento> locais;
    String[] localizacoesArray;
    int indice;

    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_eventos);


        HttpAdm requisicao = new HttpAdm();
        AuthenticationResponse authenticationResponse = (AuthenticationResponse) getIntent().getSerializableExtra("informaçõesADM");
        EventosRequest eventosRequest = new EventosRequest();



        recyclerView = findViewById(R.id.recyclerViewColoborador);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        colaboradorAdapter = new ColaboradorAdapter(this);
        recyclerView.setAdapter(colaboradorAdapter);

        Button adicionaFoto = findViewById(R.id.button2);
        EditText txtNomeEvento = findViewById(R.id.inputNome);
        EditText txtDescricao = findViewById(R.id.inputDescricao);
        Switch destacarEvento = findViewById(R.id.destaqueEvento);
        EditText txtData = findViewById(R.id.inputDate);
        EditText txtHorario = findViewById(R.id.inputTime);
        EditText txtValor = findViewById(R.id.editTextNumberedit);
        EditText txtDescontoMembro = findViewById(R.id.editTextNumber2);
        EditText txtDescontoAssociado = findViewById(R.id.editTextNumber3edit);
        AutoCompleteTextView txtEndereco = findViewById(R.id.txtEndereco);
        Button salvarButton = findViewById(R.id.button9);


        //formata os editText
        setupCurrencyFormatting(txtValor);
        setupCurrencyFormatting(txtDescontoMembro);
        setupCurrencyFormatting(txtDescontoAssociado);


        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            adicionaFoto.setBackground(new BitmapDrawable(getResources(), bitmap));
                            adicionaFoto.setText("");

                            // Convertendo a imagem para base64
                            String imagemBase64 = bitmapToBase64(bitmap);
                            eventosRequest.setImagem(imagemBase64);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

        adicionaFoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intent);
        });

        requisicao.ListLocaisEventos(authenticationResponse, new HttpAdm.HttpCallback() {
            @Override
            public void onSuccess(String response) {
                // Deserializa a resposta e prepara os dados
                Type listType = new TypeToken<List<LocalEvento>>() {}.getType();
                List<LocalEvento> responseEventos2 = new Gson().fromJson(response, listType);
                locais = responseEventos2;
                localizacoesArray = new String[locais.size()];

                // Popula o array de strings com as localizações
                for (int i = 0 ; i < locais.size(); i++) {
                    localizacoesArray[i] = locais.get(i).getLocalizacao();
                    System.out.println(localizacoesArray[i]);
                }

                // Atualiza a interface do usuário na thread principal
                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(CreateEventosActivity.this, android.R.layout.simple_dropdown_item_1line, localizacoesArray);
                    txtEndereco.setAdapter(adapter);

                    // Configura o OnItemClickListener
                    txtEndereco.setOnItemClickListener((parent, view, position, id) -> {
                        // Obtém o item selecionado e seu índice
                        String selectedItem = (String) parent.getItemAtPosition(position);
                        int selectedIndex = -1;
                        for (int i = 0; i < localizacoesArray.length; i++) {
                            if (localizacoesArray[i].equals(selectedItem)) {
                                selectedIndex = i;
                                indice = selectedIndex;
                                break;
                            }
                        }
                        // Exibe o índice selecionado
                        Log.d("SelectedIndex", "Index: " + selectedIndex);
                        // Ou você pode fazer outras ações com o índice aqui
                    });
                });
            }

            @Override
            public void onFailure(IOException e) {
                // Lida com a falha
            }
        });


        salvarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoadingDialog();

                // Obtenha o texto dos EditText e remova a formatação
                String valorEventoTexto = txtValor.getText().toString().replace("R$", "").replace(",", ".");
                String valorDescontoMembroTexto = txtDescontoMembro.getText().toString().replace("R$", "").replace(",", ".");
                String valorDescontoAssociadoTexto = txtDescontoAssociado.getText().toString().replace("R$", "").replace(",", ".");

                // Converta para Double e atribua às variáveis
                valorDoEvento = Double.parseDouble(valorEventoTexto);
                valorDescontoMembro = Double.parseDouble(valorDescontoMembroTexto);
                valorDescontoAssociado = Double.parseDouble(valorDescontoAssociadoTexto);


                System.out.println("Valor do Evento: " + valorDoEvento);
                System.out.println("Valor Desconto Membro: " + valorDescontoMembro);
                System.out.println("Valor Desconto Associado: " + valorDescontoAssociado);


                eventosRequest.setValorDoEvento(valorDoEvento);
                eventosRequest.setValorDescontoMembro(valorDescontoMembro);
                eventosRequest.setValorDescontoAssociado(valorDescontoAssociado);


                // Continue com a lógica existente para salvar os detalhes do evento
                eventosRequest.setTitulo(txtNomeEvento.getText().toString());
                eventosRequest.setDescricao(txtDescricao.getText().toString());
                eventosRequest.setDestaque(destacarEvento.isChecked());

                // Continue com a lógica existente para processar outros campos e salvar o evento
                String dataString = txtData.getText().toString();
                dataString = dataString.replace("/", "-");
                Date data = null;
                eventosRequest.setDate(data);
                eventosRequest.setHorario(txtHorario.getText().toString());
                LocalEvento local = locais.get(indice);
                eventosRequest.setLocalEvento(local);

                // Lógica para salvar colaboradores/palestrantes
                List<String> colaboradores = colaboradorAdapter.getColaboradores();
                eventosRequest.setPalestrantes(colaboradores);

                LocalEventoLocalizacaoRequest localEventoLocalizacaoRequest = new LocalEventoLocalizacaoRequest();
                localEventoLocalizacaoRequest.setLocalizacao(local.getLocalizacao());
                localEventoLocalizacaoRequest.setId(local.getId());
                eventosRequest.setLocalEventoRequest(localEventoLocalizacaoRequest);




                requisicao.criarEvento(dataString, eventosRequest, authenticationResponse, new HttpAdm.HttpCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(() -> {
                            showSuccessDialog();
                            hideLoadingDialog();
                        });

                    }

                    @Override
                    public void onFailure(IOException e) {
                        runOnUiThread(() -> {
                            showErrorDialog();
                            hideLoadingDialog();
                        });
                    }
                });
            }
        });
    }

    private void showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.loading_screen, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        loadingDialog = builder.create();
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    public String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void setupCurrencyFormatting(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    editText.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll("[R$,]", "").replace(".", "");
                    double parsed;
                    try {
                        parsed = Double.parseDouble(cleanString);
                    } catch (NumberFormatException e) {
                        parsed = 0.00;
                    }

                    String formatted = String.format(Locale.getDefault(), "%.2f", parsed / 100);

                    current = "R$ " + formatted.replace(".", ",");
                    editText.setText(current);
                    editText.setSelection(current.length());

                    editText.addTextChangedListener(this);
                }
            }
        });
    }

    private void showErrorDialog() {
        // Inflate o layout do diálogo de erro
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_erro_cadastro_evento, null);

        // Cria o dialog a partir do layout inflado
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        // Clique no botão de continuar para fechar o diálogo
        Button btnContinuar = dialogView.findViewById(R.id.buttonErrc);
        btnContinuar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }



    private void showSuccessDialog() {
        // Inflate o layout do diálogo de sucesso
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_eventocadastrado_sucesso, null);

        // Cria o dialog a partir do layout inflado
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setView(dialogView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();

        // Clique no botão de continuar para redirecionar à página de login ou outra ação
        Button btnContinuar = dialogView.findViewById(R.id.buttonEc);
        btnContinuar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                finish();

            }
        });

        dialog.show();
    }




}
