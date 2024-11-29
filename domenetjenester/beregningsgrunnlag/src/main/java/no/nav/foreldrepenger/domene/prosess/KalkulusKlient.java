package no.nav.foreldrepenger.domene.prosess;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.folketrygdloven.fpkalkulus.kontrakt.besteberegning.BesteberegningGrunnlagDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.migrering.MigrerBeregningsgrunnlagRequest;
import no.nav.vedtak.exception.TekniskException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.folketrygdloven.fpkalkulus.kontrakt.BeregnRequestDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.EnkelFpkalkulusRequestDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.HentBeregningsgrunnlagGUIRequest;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.HåndterBeregningRequestDto;
import no.nav.folketrygdloven.fpkalkulus.kontrakt.KopierBeregningsgrunnlagRequestDto;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.håndtering.OppdateringRespons;
import no.nav.foreldrepenger.domene.mappers.BeregningAksjonspunktResultatMapper;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, application = FpApplication.FPKALKULUS)
public class KalkulusKlient {
    private static final Logger LOG = LoggerFactory.getLogger(KalkulusKlient.class);

    private URI beregn;
    private URI hentGrunnlag;
    private URI hentGrunnlagGui;
    private URI hentGrunnlagBesteberegning;
    private URI kopierGrunnlag;
    private URI avklaringsbehov;
    private URI deatkvier;
    private URI avslutt;
    private URI migrer;
    private final RestClient restClient;
    private final RestConfig restConfig;

    public KalkulusKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
        this.beregn = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/beregn");
        this.hentGrunnlag = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/grunnlag");
        this.hentGrunnlagGui = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/grunnlag/gui");
        this.hentGrunnlagBesteberegning = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/grunnlag/besteberegning");
        this.kopierGrunnlag = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/kopier");
        this.avklaringsbehov = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/avklaringsbehov");
        this.deatkvier = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/deaktiver");
        this.avslutt = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/avslutt");
        this.migrer = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/migrer");
    }

    public KalkulusRespons beregn(BeregnRequestDto request) {
        var restRequest = RestRequest.newPOSTJson(request, beregn, restConfig);
        try {
            var respons = restClient.sendReturnOptional(restRequest, TilstandResponse.class).orElseThrow(() -> new IllegalStateException("Ugyldig tilstand, tomt svar fra kalkulus"));
            var aksjonspunkter = respons.getAvklaringsbehovMedTilstandDto().stream().map(BeregningAksjonspunktResultatMapper::mapKontrakt).toList();
            var vilkårRespons = respons.getVilkårResultat();
            var vilkårResponsDto = vilkårRespons == null ? null : new KalkulusRespons.VilkårRespons(vilkårRespons.getErVilkarOppfylt(),
                vilkårRespons.getRegelEvalueringSporing(), vilkårRespons.getRegelInputSporing(), vilkårRespons.getRegelVersjon());
            return new KalkulusRespons(aksjonspunkter, vilkårResponsDto);
        } catch (Exception e) {
            throw new TekniskException("FP-503800", "Feil ved kall til fpkalkulus: " + e);
        }
    }

    public Optional<BeregningsgrunnlagGrunnlagDto> hentGrunnlag(EnkelFpkalkulusRequestDto request) {
        var restRequest = RestRequest.newPOSTJson(request, hentGrunnlag, restConfig);
        return restClient.sendReturnOptional(restRequest, BeregningsgrunnlagGrunnlagDto.class);
    }

    public Optional<BeregningsgrunnlagDto> hentGrunnlagGUI(HentBeregningsgrunnlagGUIRequest request) {
        var restRequest = RestRequest.newPOSTJson(request, hentGrunnlagGui, restConfig);
        return restClient.sendReturnOptional(restRequest, BeregningsgrunnlagDto.class);
    }

    public Optional<BesteberegningGrunnlagDto> hentGrunnlagBesteberegning(EnkelFpkalkulusRequestDto request) {
        var restRequest = RestRequest.newPOSTJson(request, hentGrunnlagBesteberegning, restConfig);
        return restClient.sendReturnOptional(restRequest, BesteberegningGrunnlagDto.class);
    }

    public void kopierGrunnlag(KopierBeregningsgrunnlagRequestDto request) {
        var restRequest = RestRequest.newPOSTJson(request, kopierGrunnlag, restConfig);
        var respons = restClient.sendReturnUnhandled(restRequest);
        if (respons.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new IllegalStateException("Feil ved kopiering av grunnlag i fpkalkulus");
        }
    }

    public OppdateringRespons løsAvklaringsbehov(HåndterBeregningRequestDto request) {
        var restRequest = RestRequest.newPOSTJson(request, avklaringsbehov, restConfig);
        try {
            return restClient.sendReturnOptional(restRequest, OppdateringRespons.class).orElseThrow(() -> new IllegalStateException("Klarte ikke sende avklaringsbehov til fpkalkulus"));
        } catch (Exception e) {
            throw new TekniskException("FP-503800", "Feil ved kall til fpkalkulus: " + e);
        }

    }

    public void deaktiverGrunnlag(EnkelFpkalkulusRequestDto request) {
        var restRequest = RestRequest.newPOSTJson(request, deatkvier, restConfig);
        var respons = restClient.sendReturnUnhandled(restRequest);
        if (respons.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new IllegalStateException("Feil ved deaktivering av grunnlag i fpkalkulus");
        }
    }

    public void avsluttKobling(EnkelFpkalkulusRequestDto request) {
        var restRequest = RestRequest.newPOSTJson(request, avslutt, restConfig);
        var respons = restClient.sendReturnUnhandled(restRequest);
        if (respons.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new IllegalStateException("Feil ved avslutning av grunnlag i fpkalkulus");
        }
    }

    public void migrerGrunnlag(MigrerBeregningsgrunnlagRequest request) {
        var restRequest = RestRequest.newPOSTJson(request, migrer, restConfig);
        var respons = restClient.sendReturnUnhandled(restRequest);
        if (respons.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new IllegalStateException("Feil ved migrering av grunnlag i fpkalkulus");
        }
    }

    private URI toUri(URI endpointURI, String path) {
        try {
            return UriBuilder.fromUri(endpointURI).path(path).build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Ugyldig uri: " + endpointURI + path, e);
        }
    }


}
