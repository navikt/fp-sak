package no.nav.foreldrepenger.domene.prosess;

import static no.nav.foreldrepenger.domene.mappers.kalkulatorinput.IAYTilKalkulatorInputMapper.mapTilAktør;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.output.BeregningAvklaringsbehovResultat;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.EksternArbeidsforholdRef;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.PersonIdent;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsforholdReferanseDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.AvklaringsbehovDefinisjon;
import no.nav.folketrygdloven.kalkulus.kodeverk.StegType;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseTyperKalkulusStøtterKontrakt;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregnForRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregnListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregningsgrunnlagListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregningsgrunnlagRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagDtoForGUIRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagDtoListeForGUIRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HentBeregningsgrunnlagRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.KopierBeregningListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.KopierBeregningRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandListeResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandResponse;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.mappers.kalkulatorinput.KalkulusInputMapper;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;
import no.nav.foreldrepenger.domene.rest.KalkulusRestKlient;

@ApplicationScoped
public class KalkulusTjeneste {

    private KalkulusRestKlient kalkulusRestKlient;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    private BehandlingRepository behandlingRepository;
    private KalkulusInputMapper kalkulusInputMapper;

    public KalkulusTjeneste() {
    }

    @Inject
    public KalkulusTjeneste(KalkulusRestKlient kalkulusRestKlient,
                            InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                            OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste,
                            BehandlingRepository behandlingRepository, KalkulusInputMapper kalkulusInputMapper) {
        this.kalkulusRestKlient = kalkulusRestKlient;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.kalkulusInputMapper = kalkulusInputMapper;
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat beregn(BehandlingReferanse behandlingReferanse, BehandlingStegType behandlingStegType) {
        var request = lagBeregnRequest(behandlingReferanse, behandlingStegType);
        var respons = kalkulusRestKlient.beregn(request);
        return mapTilBeregningResultat(respons);
    }

    public Optional<BeregningsgrunnlagGrunnlagDto> hentGrunnlag(BehandlingReferanse behandlingReferanse) {
        HentBeregningsgrunnlagListeRequest hentRequest = lagHentRequest(behandlingReferanse);
        var grunnlagListe = kalkulusRestKlient.hentBeregningsgrunnlagGrunnlag(hentRequest);
        if (grunnlagListe.size() > 1) {
            throw new IllegalStateException("Forventet å finne nøyaktig ett grunnlag. Fant " + grunnlagListe.size());
        }
        return grunnlagListe.isEmpty() ? Optional.empty() : Optional.of(grunnlagListe.get(0));
    }

    public Optional<BeregningsgrunnlagDto> hentDtoForVisning(BehandlingReferanse behandlingReferanse) {
        var hentRequest = lagHentForVisningRequest(behandlingReferanse);
        var listeRespons = kalkulusRestKlient.hentBeregningsgrunnlagDto(hentRequest);
        var beregningsgrunnlagListe = listeRespons.getBeregningsgrunnlagListe();
        if (beregningsgrunnlagListe.size() > 1) {
            throw new IllegalStateException("Forventet å finne nøyaktig ett grunnlag. Fant " + beregningsgrunnlagListe.size());
        }
        return beregningsgrunnlagListe.isEmpty() ? Optional.empty() : Optional.of(beregningsgrunnlagListe.get(0).getBeregningsgrunnlag());
    }


    public void deaktiver(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        BeregningsgrunnlagListeRequest deaktiverRequest = lagDeaktiverRequest(behandling);
        kalkulusRestKlient.deaktiverBeregningsgrunnlag(deaktiverRequest);
    }

    public void kopier(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var behandlingReferanse = BehandlingReferanse.fra(behandling);
        KopierBeregningListeRequest kopierRequest = lagKopierRequest(behandling, behandlingReferanse);
        kalkulusRestKlient.kopierBeregning(kopierRequest);
    }

    private KopierBeregningListeRequest lagKopierRequest(Behandling behandling, BehandlingReferanse behandlingReferanse) {
        return new KopierBeregningListeRequest(behandlingReferanse.getSaksnummer().getVerdi(),
            YtelseTyperKalkulusStøtterKontrakt.fraKode(behandling.getFagsakYtelseType().getKode()),
            List.of(new KopierBeregningRequest(behandling.getUuid(), finnOriginalReferanser(behandlingReferanse).get(0))));
    }


    private BeregnListeRequest lagBeregnRequest(BehandlingReferanse behandlingReferanse, BehandlingStegType behandlingStegType) {
        return new BeregnListeRequest(behandlingReferanse.getSaksnummer().getVerdi(), map(behandlingReferanse),
            YtelseTyperKalkulusStøtterKontrakt.fraKode(behandlingReferanse.getFagsakYtelseType().getKode()),
            new StegType(behandlingStegType.getKode()), List.of(mapBeregnRequestForBehandling(behandlingReferanse)));
    }

    private BeregningsgrunnlagListeRequest lagDeaktiverRequest(Behandling behandling) {
        return new BeregningsgrunnlagListeRequest(behandling.getFagsak().getSaksnummer().getVerdi(),
            List.of(new BeregningsgrunnlagRequest(behandling.getUuid())));
    }

    private HentBeregningsgrunnlagListeRequest lagHentRequest(BehandlingReferanse behandlingReferanse) {
        return new HentBeregningsgrunnlagListeRequest(List.of(new HentBeregningsgrunnlagRequest(behandlingReferanse.getBehandlingUuid(), behandlingReferanse.getSaksnummer().getVerdi(), YtelseTyperKalkulusStøtterKontrakt.fraKode(
            behandlingReferanse.getFagsakYtelseType().getKode()))),
            behandlingReferanse.getBehandlingUuid(), behandlingReferanse.getSaksnummer().getVerdi(), false);
    }

    private HentBeregningsgrunnlagDtoListeForGUIRequest lagHentForVisningRequest(BehandlingReferanse behandlingReferanse) {
        Set<ArbeidsforholdReferanseDto> referanser = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId()).getArbeidsforholdInformasjon()
            .stream()
            .map(ArbeidsforholdInformasjon::getArbeidsforholdReferanser)
            .flatMap(Collection::stream)
            .map(ref -> new ArbeidsforholdReferanseDto(mapTilAktør(ref.getArbeidsgiver()),
                new InternArbeidsforholdRefDto(ref.getInternReferanse().getReferanse()),
                new EksternArbeidsforholdRef(ref.getEksternReferanse().getReferanse())))
            .collect(Collectors.toSet());
        return new HentBeregningsgrunnlagDtoListeForGUIRequest(
            List.of(new HentBeregningsgrunnlagDtoForGUIRequest(
                behandlingReferanse.getBehandlingUuid(),
                YtelseTyperKalkulusStøtterKontrakt.fraKode(behandlingReferanse.getFagsakYtelseType().getKode()),
                referanser,
                behandlingReferanse.getSkjæringstidspunkt().getSkjæringstidspunktOpptjening()
                )),
            behandlingReferanse.getBehandlingUuid());
    }

    private BeregningsgrunnlagVilkårOgAkjonspunktResultat mapTilBeregningResultat(TilstandListeResponse respons) {
        if (respons.getTilstand().size() != 1) {
            throw new IllegalStateException("Forventet å få akkurat en respons fra kalkulus, fikk " + respons.getTilstand().size());
        }
        var tilstandRespons = respons.getTilstand().get(0);
        var aksjonspunktliste = mapAvklaringsbehovResultatFraRespons(tilstandRespons);
        var resultat = new BeregningsgrunnlagVilkårOgAkjonspunktResultat(aksjonspunktliste);
        if (tilstandRespons.getVilkarOppfylt() != null) {
            // TODO trenger vi regelevaluering for beregning lagret i fp-sak? Brukes i generering av vedtak-xml
            resultat.setVilkårOppfylt(tilstandRespons.getVilkarOppfylt(), "", "");
        }
        return resultat;
    }


    private List<BeregningAvklaringsbehovResultat> mapAvklaringsbehovResultatFraRespons(TilstandResponse tilstandRespons) {
        var avklaringsbehovliste = new ArrayList<BeregningAvklaringsbehovResultat>();
        if (tilstandRespons.getAvklaringsbehovMedTilstandDto() != null) {
            tilstandRespons.getAvklaringsbehovMedTilstandDto().stream().
                map(ab -> BeregningAvklaringsbehovResultat.opprettFor(AvklaringsbehovDefinisjon.fraKode(ab.getBeregningAvklaringsbehovDefinisjon().getKode())))
                .forEach(avklaringsbehovliste::add);
        }
        return avklaringsbehovliste;
    }

    private BeregnForRequest mapBeregnRequestForBehandling(BehandlingReferanse behandlingReferanse) {
        var originalreferanser = finnOriginalReferanser(behandlingReferanse);

        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId());
        var opptjeningAktiviteter = opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(behandlingReferanse, iayGrunnlag).orElseThrow(() -> new IllegalStateException("Ingen opptjeningsaktiviteter for beregning"));
        var kalkulatorInput = kalkulusInputMapper.mapKalkulatorInput(behandlingReferanse, iayGrunnlag, opptjeningAktiviteter);
        return new BeregnForRequest(behandlingReferanse.getBehandlingUuid(), originalreferanser, kalkulatorInput, Collections.emptyList());
    }

    private List<UUID> finnOriginalReferanser(BehandlingReferanse behandlingReferanse) {
        return behandlingReferanse.getOriginalBehandlingId()
            .map(behandlingRepository::hentBehandling)
            .map(Behandling::getUuid)
            .map(List::of)
            .orElse(Collections.emptyList());
    }

    private PersonIdent map(BehandlingReferanse behandlingReferanse) {
        return new AktørIdPersonident(behandlingReferanse.getAktørId().getId());
    }

}
