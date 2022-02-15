package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.mappers.endringutleder.UtledEndring;
import no.nav.foreldrepenger.domene.mappers.fra_entitet_til_modell.FraEntitetTilBehandlingsmodellMapper;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.OppdatererDtoMapper;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.BeregningDtoTjeneste;
import no.nav.foreldrepenger.domene.rest.BeregningHåndterer;
import no.nav.foreldrepenger.domene.rest.dto.AvklarteAktiviteterDto;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class BeregningFPSAK implements BeregningAPI {

    private BehandlingRepository behandlingRepository;
    private Instance<SkjæringstidspunktTjeneste> skjæringstidspunktTjeneste;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private HentOgLagreBeregningsgrunnlagTjeneste hentBeregningsgrunnlagTjeneste;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;
    private BeregningDtoTjeneste beregningDtoTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BeregningHåndterer beregningHåndterer;

    public BeregningFPSAK() {
    }

    @Inject
    public BeregningFPSAK(BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                          BehandlingRepository behandlingRepository,
                          Instance<SkjæringstidspunktTjeneste> skjæringstidspunktTjeneste,
                          HentOgLagreBeregningsgrunnlagTjeneste hentBeregningsgrunnlagTjeneste,
                          BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                          BeregningsgrunnlagInputProvider inputTjenesteProvider,
                          BeregningDtoTjeneste beregningDtoTjeneste,
                          InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                          BeregningHåndterer beregningHåndterer) {
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.hentBeregningsgrunnlagTjeneste = hentBeregningsgrunnlagTjeneste;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.beregningsgrunnlagInputProvider = Objects.requireNonNull(inputTjenesteProvider, "inputTjenesteProvider");
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.beregningDtoTjeneste = beregningDtoTjeneste;
        this.beregningHåndterer = beregningHåndterer;
    }


    /**
     * Kjører beregning for angitt steg
     *
     * @param behandlingId       behandlingId
     * @param behandlingStegType Stegtype
     * @return Resultatstruktur med aksjonspunkter og eventuell vilkårsvurdering
     */
    @Override
    public BeregningsgrunnlagVilkårOgAkjonspunktResultat beregn(Long behandlingId, BehandlingStegType behandlingStegType) {
        var behandlingReferanse = lagReferanseMedSkjæringstidspunkt(behandlingId);
        return beregnUtenKalkulus(behandlingReferanse, behandlingStegType);
    }

    @Override
    public OppdaterBeregningsgrunnlagResultat oppdater(AksjonspunktOppdaterParameter param, BekreftetAksjonspunktDto bekreftAksjonspunktDto) {
        Optional<BeregningsgrunnlagGrunnlagEntitet> forrige;
        BeregningsgrunnlagGrunnlagEntitet stegGrunnlag;
        if (bekreftAksjonspunktDto instanceof AvklarteAktiviteterDto dto) {
            var tjeneste = beregningsgrunnlagInputProvider.getTjeneste(param.getRef().getFagsakYtelseType());
            var inputUtenBeregningsgrunnlag = tjeneste.lagInput(param.getRef());
            forrige = hentBeregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitet(param.getBehandlingId(),
                BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
            stegGrunnlag = hentBeregningsgrunnlagTjeneste.hentSisteBeregningsgrunnlagGrunnlagEntitet(param.getBehandlingId(),
                BeregningsgrunnlagTilstand.OPPRETTET).orElseThrow();
            beregningHåndterer.håndterAvklarAktiviteter(inputUtenBeregningsgrunnlag, OppdatererDtoMapper.mapAvklarteAktiviteterDto(dto));
        } else {
            throw new IllegalStateException("Ugyldig aksjonspunkt for beregning");
        }
        var nyttGrunnlag = hentBeregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(param.getBehandlingId()).orElseThrow();
        return UtledEndring.utled(nyttGrunnlag, stegGrunnlag, forrige, bekreftAksjonspunktDto,
            inntektArbeidYtelseTjeneste.hentGrunnlag(param.getBehandlingId()));
    }

    /**
     * Henter beregningsgrunnlag
     *
     * @param behandlingId behandlingId
     * @return BeregningsgrunnlagGrunnlag
     */
    @Override
    public Optional<BeregningsgrunnlagGrunnlag> hent(Long behandlingId) {
        return hentBeregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId)
            .map(FraEntitetTilBehandlingsmodellMapper::mapBeregningsgrunnlagGrunnlag);
    }

    @Override
    public Optional<BeregningsgrunnlagDto> hentForGUI(Long behandlingId) {
        var behandlingReferanse = lagReferanseMedSkjæringstidspunkt(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var inputTjeneste = beregningsgrunnlagInputProvider.getRestInputTjeneste(behandlingReferanse.getFagsakYtelseType());
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId);
        var input = inputTjeneste.lagInput(behandling, inntektArbeidYtelseGrunnlag);
        return input.flatMap(i -> beregningDtoTjeneste.lagBeregningsgrunnlagDto(i));
    }

    /**
     * Kopierer beregningsgrunnlag
     *
     * @param behandlingId behandlingId
     */
    @Override
    public void kopierFastsatt(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var originalBehandlingId = behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Kan ikke kopiere uten original behandling"));
        beregningsgrunnlagKopierOgLagreTjeneste.kopierBeregningsresultatFraOriginalBehandling(originalBehandlingId, behandlingId);
    }

    /**
     * Rydder beregningsgrunnlag og tilhørende resultat for et hopp bakover kall i oppgitt steg
     *
     * @param kontekst           Behandlingskontrollkontekst
     * @param behandlingStegType steget ryddkallet kjøres fra
     * @param tilSteg            Siste steg i hopp bakover transisjonen
     */
    @Override
    public void rydd(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType, BehandlingStegType tilSteg) {
        ryddUtenKalkulus(kontekst, behandlingStegType, tilSteg);
    }

    private BeregningsgrunnlagVilkårOgAkjonspunktResultat beregnUtenKalkulus(BehandlingReferanse behandlingReferanse,
                                                                             BehandlingStegType behandlingStegType) {
        var inputTjeneste = beregningsgrunnlagInputProvider.getTjeneste(behandlingReferanse.getFagsakYtelseType());
        var input = inputTjeneste.lagInput(behandlingReferanse.getBehandlingId());
        switch (behandlingStegType) {
            case FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING:
                var aksjonspunktListe = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);
                return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(aksjonspunktListe);
            case KONTROLLER_FAKTA_BEREGNING:
                aksjonspunktListe = beregningsgrunnlagKopierOgLagreTjeneste.kontrollerFaktaBeregningsgrunnlag(input);
                return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(aksjonspunktListe);
            case FORESLÅ_BEREGNINGSGRUNNLAG:
                return beregningsgrunnlagKopierOgLagreTjeneste.foreslåBeregningsgrunnlag(input);
            case FORESLÅ_BESTEBEREGNING:
                return beregningsgrunnlagKopierOgLagreTjeneste.foreslåBesteberegning(input);
            case VURDER_VILKAR_BERGRUNN:
                return beregningsgrunnlagKopierOgLagreTjeneste.vurderVilkårBeregningsgrunnlag(input);
            case VURDER_REF_BERGRUNN:
                return beregningsgrunnlagKopierOgLagreTjeneste.vurderRefusjonBeregningsgrunnlag(input);
            case FORDEL_BEREGNINGSGRUNNLAG:
                return beregningsgrunnlagKopierOgLagreTjeneste.fordelBeregningsgrunnlag(input);
            case FASTSETT_BEREGNINGSGRUNNLAG:
                beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsgrunnlag(input);
                return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(Collections.emptyList());
            default:
                throw new IllegalStateException("Ugyldig steg for beregning " + behandlingStegType);
        }
    }


    private void ryddUtenKalkulus(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType, BehandlingStegType tilSteg) {
        if (!tilSteg.equals(behandlingStegType)) {
            switch (behandlingStegType) {
                case FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING:
                    beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddFastsettSkjæringstidspunktVedTilbakeføring();
                case VURDER_VILKAR_BERGRUNN:
                    beregningsgrunnlagVilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst);
            }
        } else {
            switch (behandlingStegType) {
                case FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING:
                    beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst)
                        .gjenopprettFastsattBeregningAktivitetBeregningsgrunnlag();
                case KONTROLLER_FAKTA_BEREGNING:
                    beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).gjenopprettOppdatertBeregningsgrunnlag();
                case FORESLÅ_BEREGNINGSGRUNNLAG:
                    beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddForeslåBeregningsgrunnlagVedTilbakeføring();
                case FORESLÅ_BESTEBEREGNING:
                    beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddForeslåBesteberegningVedTilbakeføring();
                case VURDER_VILKAR_BERGRUNN:
                    beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddVurderVilkårBeregningsgrunnlagVedTilbakeføring();
                case VURDER_REF_BERGRUNN:
                    beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst)
                        .ryddVurderRefusjonBeregningsgrunnlagVedTilbakeføring();
                case FORDEL_BEREGNINGSGRUNNLAG:
                    var aps = behandlingRepository.hentBehandling(kontekst.getBehandlingId()).getAksjonspunkter();
                    var harAksjonspunktSomErUtførtIUtgang = tilSteg.getAksjonspunktDefinisjonerUtgang()
                        .stream()
                        .anyMatch(ap -> aps.stream().filter(a -> a.getAksjonspunktDefinisjon().equals(ap)).anyMatch(a -> !a.erAvbrutt()));
                    beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst)
                        .ryddFordelBeregningsgrunnlagVedTilbakeføring(harAksjonspunktSomErUtførtIUtgang);
            }
        }
    }

    private BehandlingReferanse lagReferanseMedSkjæringstidspunkt(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkt = getSkjæringstidspunkt(behandlingId, behandling.getFagsakYtelseType());
        return BehandlingReferanse.fra(behandling).medSkjæringstidspunkt(skjæringstidspunkt);
    }

    private Skjæringstidspunkt getSkjæringstidspunkt(Long behandlingId, FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(skjæringstidspunktTjeneste, fagsakYtelseType)
            .map(t -> t.getSkjæringstidspunkter(behandlingId))
            .orElse(null);
    }


}
