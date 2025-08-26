package no.nav.foreldrepenger.domene.prosess;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.folketrygdloven.kalkulus.request.v1.enkel.EnkelBeregnRequestDto;
import no.nav.folketrygdloven.kalkulus.request.v1.enkel.EnkelFpkalkulusRequestDto;
import no.nav.folketrygdloven.kalkulus.request.v1.enkel.EnkelGrunnlagTilstanderRequestDto;
import no.nav.folketrygdloven.kalkulus.request.v1.enkel.EnkelHentBeregningsgrunnlagGUIRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.enkel.EnkelHåndterBeregningRequestDto;
import no.nav.folketrygdloven.kalkulus.request.v1.enkel.EnkelKopierBeregningsgrunnlagRequestDto;
import no.nav.folketrygdloven.kalkulus.request.v1.enkel.KopierFastsattGrunnlagRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.besteberegning.BesteberegningGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.håndtering.OppdateringRespons;
import no.nav.folketrygdloven.kalkulus.response.v1.tilstander.TilgjengeligeTilstanderDto;
import no.nav.foreldrepenger.domene.mappers.BeregningAksjonspunktResultatMapper;
import no.nav.vedtak.exception.TekniskException;
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
    private URI kopierFastsattGrunnlag;
    private URI avklaringsbehov;
    private URI deatkvier;
    private URI tilstand;
    private URI avslutt;
    private final RestClient restClient;
    private final RestConfig restConfig;

    public KalkulusKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
        this.beregn = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/beregn");
        this.hentGrunnlag = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/grunnlag");
        this.hentGrunnlagGui = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/grunnlag/gui");
        this.hentGrunnlagBesteberegning = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/grunnlag/besteberegning");
        this.tilstand = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/grunnlag/tilstander");
        this.kopierGrunnlag = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/kopier");
        this.kopierFastsattGrunnlag = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/kopier-fastsatt");
        this.avklaringsbehov = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/avklaringsbehov");
        this.deatkvier = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/deaktiver");
        this.avslutt = toUri(restConfig.fpContextPath(), "/api/kalkulus/v1/avslutt");
    }

    public KalkulusRespons beregn(EnkelBeregnRequestDto request) {
        LOG.info("Kjører beregning i fpkalkulus for steg {}", request.stegType());
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
        LOG.info("Henter grunnlag fra fpkalkulus");
        var restRequest = RestRequest.newPOSTJson(request, hentGrunnlag, restConfig);
        return restClient.sendReturnOptional(restRequest, BeregningsgrunnlagGrunnlagDto.class);
    }

    public Optional<BeregningsgrunnlagDto> hentGrunnlagGUI(EnkelHentBeregningsgrunnlagGUIRequest request) {
        LOG.info("Henter GUI dto fra fpkalkulus");
        var restRequest = RestRequest.newPOSTJson(request, hentGrunnlagGui, restConfig);
        return restClient.sendReturnOptional(restRequest, BeregningsgrunnlagDto.class);
    }

    public Optional<BesteberegningGrunnlagDto> hentGrunnlagBesteberegning(EnkelFpkalkulusRequestDto request) {
        LOG.info("Henter besteberegningsgrunnlag fra fpkalkulus");
        var restRequest = RestRequest.newPOSTJson(request, hentGrunnlagBesteberegning, restConfig);
        return restClient.sendReturnOptional(restRequest, BesteberegningGrunnlagDto.class);
    }

    public void kopierGrunnlag(EnkelKopierBeregningsgrunnlagRequestDto request) {
        LOG.info("Kopierer grunnlag i fpkalkulus");
        var restRequest = RestRequest.newPOSTJson(request, kopierGrunnlag, restConfig);
        var respons = restClient.sendReturnUnhandled(restRequest);
        if (respons.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new IllegalStateException("Feil ved kopiering av grunnlag i fpkalkulus");
        }
    }

    public void kopierFastsattGrunnlag(KopierFastsattGrunnlagRequest request) {
        LOG.info("Kopierer fastsatt grunnlag i fpkalkulus");
        var restRequest = RestRequest.newPOSTJson(request, kopierFastsattGrunnlag, restConfig);
        var respons = restClient.sendReturnUnhandled(restRequest);
        if (respons.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new IllegalStateException("Feil ved kopiering av fastsatt grunnlag i fpkalkulus");
        }
    }

    public OppdateringRespons løsAvklaringsbehov(EnkelHåndterBeregningRequestDto request) {
        LOG.info("Løser avklaringsbehov i fpkalkulus");
        var restRequest = RestRequest.newPOSTJson(request, avklaringsbehov, restConfig);
        try {
            return restClient.sendReturnOptional(restRequest, OppdateringRespons.class).orElseThrow(() -> new IllegalStateException("Klarte ikke sende avklaringsbehov til fpkalkulus"));
        } catch (Exception e) {
            throw new TekniskException("FP-503800", "Feil ved kall til fpkalkulus: " + e);
        }
    }

    public void deaktiverGrunnlag(EnkelFpkalkulusRequestDto request) {
        LOG.info("Deaktiverer grunnlag i fpkalkulus");
        var restRequest = RestRequest.newPOSTJson(request, deatkvier, restConfig);
        var respons = restClient.sendReturnUnhandled(restRequest);
        if (respons.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new IllegalStateException("Feil ved deaktivering av grunnlag i fpkalkulus");
        }
    }

    public void avsluttKobling(EnkelFpkalkulusRequestDto request) {
        LOG.info("Avslutter kobling i fpkalkulus");
        var restRequest = RestRequest.newPOSTJson(request, avslutt, restConfig);
        var respons = restClient.sendReturnUnhandled(restRequest);
        if (respons.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new IllegalStateException("Feil ved avslutning av grunnlag i fpkalkulus");
        }
    }

    public TilgjengeligeTilstanderDto hentTilgjengeligeTilstander(EnkelGrunnlagTilstanderRequestDto request) {
        LOG.info("Henter tilgjengelige tilstander i fpkalkulus");
        var restRequest = RestRequest.newPOSTJson(request, tilstand, restConfig);
        try {
            return restClient.sendReturnOptional(restRequest, TilgjengeligeTilstanderDto.class).orElseThrow(() -> new IllegalStateException("Klarte ikke hente tilgjengelige tilstander fra fpkalkulus"));
        }
        catch (Exception e) {
            throw new TekniskException("FP-503900", "Feil under migrering til kalkulus: " + e);
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
