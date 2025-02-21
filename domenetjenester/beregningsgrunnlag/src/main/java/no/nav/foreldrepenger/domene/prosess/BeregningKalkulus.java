package no.nav.foreldrepenger.domene.prosess;

import java.util.Collections;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulus.request.v1.enkel.EnkelBeregnRequestDto;
import no.nav.folketrygdloven.kalkulus.request.v1.enkel.EnkelFpkalkulusRequestDto;

import no.nav.folketrygdloven.kalkulus.request.v1.enkel.EnkelHentBeregningsgrunnlagGUIRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.enkel.EnkelHåndterBeregningRequestDto;
import no.nav.folketrygdloven.kalkulus.request.v1.enkel.EnkelKopierBeregningsgrunnlagRequestDto;
import no.nav.folketrygdloven.kalkulus.response.v1.besteberegning.BesteberegningGrunnlagDto;

import org.jboss.weld.exceptions.IllegalStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.Saksnummer;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningSteg;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OverstyringAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.aksjonspunkt.KalkulusAksjonspunktMapper;
import no.nav.foreldrepenger.domene.aksjonspunkt.MapEndringsresultat;
import no.nav.foreldrepenger.domene.aksjonspunkt.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagKobling;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagKoblingRepository;
import no.nav.foreldrepenger.domene.fp.BesteberegningFødendeKvinneTjeneste;
import no.nav.foreldrepenger.domene.mappers.KalkulusInputTjeneste;
import no.nav.foreldrepenger.domene.mappers.fra_kalkulus_til_domene.KalkulusTilFpsakMapper;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class BeregningKalkulus implements BeregningAPI {
    private static final Logger LOG = LoggerFactory.getLogger(BeregningKalkulus.class);

    private KalkulusKlient klient;
    private KalkulusInputTjeneste kalkulusInputTjeneste;
    private BeregningsgrunnlagKoblingRepository koblingRepository;
    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    BeregningKalkulus() {
        // CDI
    }

    @Inject
    public BeregningKalkulus(KalkulusKlient klient,
                             KalkulusInputTjeneste kalkulusInputTjeneste,
                             BeregningsgrunnlagKoblingRepository koblingRepository,
                             BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste,
                             SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.klient = klient;
        this.kalkulusInputTjeneste = kalkulusInputTjeneste;
        this.koblingRepository = koblingRepository;
        this.besteberegningFødendeKvinneTjeneste = besteberegningFødendeKvinneTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public Optional<BeregningsgrunnlagGrunnlag> hent(BehandlingReferanse referanse) {
        var kobling = koblingRepository.hentKobling(referanse.behandlingId());
        return kobling.flatMap(k -> {
            var request = new EnkelFpkalkulusRequestDto(k.getKoblingUuid(), new Saksnummer(referanse.saksnummer().getVerdi()));
            var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(referanse.behandlingId());
            var måHenteBesteberegningsgrunnlag = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(referanse, stp);
            Optional<BesteberegningGrunnlagDto> besteberegnetGrunnlag = måHenteBesteberegningsgrunnlag ? klient.hentGrunnlagBesteberegning(request) : Optional.empty();
            return klient.hentGrunnlag(request).map(bgDto -> KalkulusTilFpsakMapper.map(bgDto, besteberegnetGrunnlag));
        });
    }

    @Override
    public BeregningsgrunnlagVilkårOgAkjonspunktResultat beregn(BehandlingReferanse behandlingReferanse, BehandlingStegType stegType) {
        var koblingOpt = koblingRepository.hentKobling(behandlingReferanse.behandlingId());
        if (koblingOpt.isEmpty() && !stegType.equals(BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING)) {
            throw new IllegalStateException("Kan ikke opprette ny kobling uten at denne starter i første steg av beregning, angitt steg var " + stegType);
        }
        var kobling = koblingOpt.orElseGet(() -> koblingRepository.opprettKobling(behandlingReferanse));
        var beregningSteg = mapTilBeregningStegType(stegType);
        var originalKobling = behandlingReferanse.getOriginalBehandlingId().flatMap(oid -> koblingRepository.hentKobling(oid));
        var request = lagBeregningRequest(behandlingReferanse, kobling, beregningSteg, originalKobling);
        var respons = klient.beregn(request);
        var prosessResultat = new BeregningsgrunnlagVilkårOgAkjonspunktResultat(respons.aksjonspunkter());
        if (respons.vilkårdata() != null) {
            prosessResultat.setVilkårOppfylt(respons.vilkårdata().erVilkårOppfylt(), respons.vilkårdata().regelEvalueringSporing(), respons.vilkårdata().regelInputSporing(), respons.vilkårdata().regelVersjon());
        }
        oppdaterKoblingMedData(behandlingReferanse, stegType, kobling);
        return prosessResultat;
    }

    private void oppdaterKoblingMedData(BehandlingReferanse behandlingReferanse, BehandlingStegType stegType, BeregningsgrunnlagKobling kobling) {
        if (stegType.equals(BehandlingStegType.KONTROLLER_FAKTA_BEREGNING)) {
            var bg = hent(behandlingReferanse).flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag).orElseThrow();
            koblingRepository.oppdaterKoblingMedStpOgGrunnbeløp(kobling, Beløp.fra(bg.getGrunnbeløp().getVerdi()), bg.getSkjæringstidspunkt());
        } else if (stegType.equals(BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG)) {
            var gr = hent(behandlingReferanse).orElseThrow();
            var kanVærePåvirketAvRegulering = gr.getBeregningsgrunnlag().map(GrunnbeløpReguleringsutleder::kanPåvirkesAvGrunnbeløpRegulering).orElse(false);
            koblingRepository.oppdaterKoblingMedReguleringsbehov(kobling, kanVærePåvirketAvRegulering);
        }
    }

    @Override
    public Optional<BeregningsgrunnlagDto> hentGUIDto(BehandlingReferanse referanse) {
        var kobling = koblingRepository.hentKobling(referanse.behandlingId());
        return kobling.flatMap(k -> {
            var kalkulusInput = kalkulusInputTjeneste.lagKalkulusInput(referanse);
            var hentGuiDtoRequest = new EnkelHentBeregningsgrunnlagGUIRequest(k.getKoblingUuid(),
                new Saksnummer(referanse.saksnummer().getVerdi()), kalkulusInput);
            return klient.hentGrunnlagGUI(hentGuiDtoRequest);
        });
    }

    @Override
    public void kopier(BehandlingReferanse revurdering, BehandlingReferanse originalbehandling, BeregningsgrunnlagTilstand tilstand) {
        if (!revurdering.saksnummer().equals(originalbehandling.saksnummer())) {
            throw new IllegalStateException("Prøver å kopiere fra et grunnlag uten samme saksnummer, ugyldig handling");
        }
        var originalKobling = koblingRepository.hentKobling(originalbehandling.behandlingId());
        if (originalKobling.isEmpty()) {
            // Forrige behandling hadde ikke noe beregningsgrunnlag, ingenting å kopiere
            return;
        }
        var koblingOpt = koblingRepository.hentKobling(revurdering.behandlingId());
        var kobling = koblingOpt.orElseGet(() -> {
            LOG.info("Kobling for behandlingUuid {} finnes ikke, oppretter", revurdering.behandlingUuid());
            return koblingRepository.opprettKoblingFraOriginal(revurdering, originalKobling.get());
        });
        if (!tilstand.equals(BeregningsgrunnlagTilstand.FASTSATT)) {
            throw new IllegalStateException("Støtter ikke kopiering av grunnlag som ikke er fastsatt!");
        }
        var request = lagKopierRequest(revurdering.saksnummer().getVerdi(), kobling, originalKobling.get());
        klient.kopierGrunnlag(request);
    }

    @Override
    public Optional<OppdaterBeregningsgrunnlagResultat> oppdaterBeregning(BekreftetAksjonspunktDto oppdatering, BehandlingReferanse referanse) {
        var kalkulusDtoer = KalkulusAksjonspunktMapper.mapAksjonspunktTilKalkulusDto(oppdatering);
        return utførOppdatering(referanse, kalkulusDtoer);
    }

    @Override
    public Optional<OppdaterBeregningsgrunnlagResultat> overstyrBeregning(OverstyringAksjonspunktDto overstyring, BehandlingReferanse referanse) {
        var kalkulusDtoer = KalkulusAksjonspunktMapper.mapOverstyringTilKalkulusDto(overstyring);
        return utførOppdatering(referanse, kalkulusDtoer);
    }

    @Override
    public void avslutt(BehandlingReferanse referanse) {
        koblingRepository.hentKobling(referanse.behandlingId())
            .ifPresent(k -> {
                LOG.info("Lukker kalkuluskobling med koblingUuid {}", k.getKoblingUuid());
                klient.avsluttKobling(new EnkelFpkalkulusRequestDto(k.getKoblingUuid(), new Saksnummer(referanse.saksnummer().getVerdi())));
            });
    }

    private Optional<OppdaterBeregningsgrunnlagResultat> utførOppdatering(BehandlingReferanse referanse,
                                                                          HåndterBeregningDto kalkulusDtoer) {
        var kobling = koblingRepository.hentKobling(referanse.behandlingId())
            .orElseThrow(() -> new IllegalStateException("Kan ikke løse aksjonspunkter i beregning uten først å ha opprettet kobling!"));
        var request = new EnkelHåndterBeregningRequestDto(kobling.getKoblingUuid(), new Saksnummer(referanse.saksnummer().getVerdi()),
            kalkulusInputTjeneste.lagKalkulusInput(referanse), Collections.singletonList(kalkulusDtoer));
        var respons = klient.løsAvklaringsbehov(request);
        return Optional.of(MapEndringsresultat.mapFraOppdateringRespons(respons));
    }

    private EnkelKopierBeregningsgrunnlagRequestDto lagKopierRequest(String verdi, BeregningsgrunnlagKobling kobling, BeregningsgrunnlagKobling originalKobling) {
        return new EnkelKopierBeregningsgrunnlagRequestDto(new Saksnummer(verdi), kobling.getKoblingUuid(),
            originalKobling.getKoblingUuid(), BeregningSteg.FAST_BERGRUNN);
    }

    private no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType mapYtelseSomSkalBeregnes(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType.SVANGERSKAPSPENGER;
            case ENGANGSTØNAD, UDEFINERT -> throw new IllegalStateException("Ukjent ytelse som skal beregnes " + fagsakYtelseType);
        };
    }

    private BeregningSteg mapTilBeregningStegType(BehandlingStegType stegType) {
        return switch (stegType) {
            case FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING -> BeregningSteg.FASTSETT_STP_BER;
            case KONTROLLER_FAKTA_BEREGNING -> BeregningSteg.KOFAKBER;
            case FORESLÅ_BEREGNINGSGRUNNLAG -> BeregningSteg.FORS_BERGRUNN;
            case FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG -> BeregningSteg.FORS_BERGRUNN_2;
            case FORESLÅ_BESTEBEREGNING -> BeregningSteg.FORS_BESTEBEREGNING;
            case VURDER_VILKAR_BERGRUNN -> BeregningSteg.VURDER_VILKAR_BERGRUNN;
            case VURDER_REF_BERGRUNN -> BeregningSteg.VURDER_REF_BERGRUNN;
            case FORDEL_BEREGNINGSGRUNNLAG -> BeregningSteg.FORDEL_BERGRUNN;
            case FASTSETT_BEREGNINGSGRUNNLAG -> BeregningSteg.FAST_BERGRUNN;
            default -> throw new IllegalStateException("Ukjent beregningssteg " + stegType);
        };
    }

    private EnkelBeregnRequestDto lagBeregningRequest(BehandlingReferanse behandlingReferanse, BeregningsgrunnlagKobling kobling, BeregningSteg beregningSteg,
                                                      Optional<BeregningsgrunnlagKobling> originalKobling) {
        var saksnummer = new Saksnummer(behandlingReferanse.saksnummer().getVerdi());
        var personIdent = new AktørIdPersonident(behandlingReferanse.aktørId().getId());
        var ytelse = mapYtelseSomSkalBeregnes(behandlingReferanse.fagsakYtelseType());
        var input = kalkulusInputTjeneste.lagKalkulusInput(behandlingReferanse);
        return new EnkelBeregnRequestDto(saksnummer, kobling.getKoblingUuid(), personIdent, ytelse, beregningSteg, input, originalKobling.map(BeregningsgrunnlagKobling::getKoblingUuid).orElse(null));
    }
}
