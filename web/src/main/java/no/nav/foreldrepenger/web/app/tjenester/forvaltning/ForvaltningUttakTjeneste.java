package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.InngangsvilkårGrunnlagBygger.adopsjonsvilkåretOppfylt;
import static no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.InngangsvilkårGrunnlagBygger.foreldreansvarsvilkåretOppfylt;
import static no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.InngangsvilkårGrunnlagBygger.fødselsvilkårOppfylt;
import static no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.InngangsvilkårGrunnlagBygger.opptjeningsvilkåretOppfylt;

import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.BeregnStønadskontoerTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.BekreftFaktaForOmsorgVurderingAksjonspunktDto;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
public class ForvaltningUttakTjeneste {

    private FpUttakRepository fpUttakRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private FagsakRepository fagsakRepository;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private HistorikkRepository historikkRepository;

    @Inject
    public ForvaltningUttakTjeneste(FpUttakRepository fpUttakRepository,
                                    BehandlingRepository behandlingRepository,
                                    BehandlingsresultatRepository behandlingsresultatRepository,
                                    BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                    VilkårResultatRepository vilkårResultatRepository,
                                    BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste,
                                    UttakInputTjeneste uttakInputTjeneste,
                                    FagsakRelasjonRepository fagsakRelasjonRepository,
                                    FagsakRepository fagsakRepository,
                                    YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                    HistorikkRepository historikkRepository) {
        this.fpUttakRepository = fpUttakRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.beregnStønadskontoerTjeneste = beregnStønadskontoerTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
        this.fagsakRepository = fagsakRepository;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.historikkRepository = historikkRepository;
    }

    ForvaltningUttakTjeneste() {
        //CDI
    }

    boolean erFerdigForeldrepengerBehandlingSomHarFørtTilOpphør(UUID behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        return behandling.getStatus().erFerdigbehandletStatus() && FagsakYtelseType.FORELDREPENGER.equals(
            behandling.getFagsakYtelseType()) && behandlingsresultat.orElseThrow().isBehandlingsresultatOpphørt();
    }

    void lagOpphørtUttaksresultat(UUID behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        kopierBeregningsgrunnlag(behandling);

        var forrigeUttaksresultat = hentForrigeUttaksresultat(behandling);
        var perioder = new UttakResultatPerioderEntitet();
        for (var uttaksperiode : forrigeUttaksresultat.getGjeldendePerioder().getPerioder()) {
            perioder.leggTilPeriode(opphørPeriode(behandling, uttaksperiode));
        }

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), perioder);
    }

    private void kopierBeregningsgrunnlag(Behandling behandling) {
        behandling.getOriginalBehandlingId()
            .ifPresent(
                originalId -> beregningsgrunnlagKopierOgLagreTjeneste.kopierBeregningsresultatFraOriginalBehandling(
                    originalId, behandling.getId()));
    }

    private UttakResultatPeriodeEntitet opphørPeriode(Behandling behandling, UttakResultatPeriodeEntitet periode) {
        var builder = new UttakResultatPeriodeEntitet.Builder(periode.getFom(),
            periode.getTom()).medResultatType(PeriodeResultatType.AVSLÅTT, opphørAvslagsårsak(behandling))
            .medUtsettelseType(periode.getUtsettelseType())
            .medOppholdÅrsak(periode.getOppholdÅrsak())
            .medOverføringÅrsak(periode.getOverføringÅrsak())
            .medSamtidigUttak(periode.isSamtidigUttak())
            .medSamtidigUttaksprosent(periode.getSamtidigUttaksprosent())
            .medFlerbarnsdager(periode.isFlerbarnsdager());
        if (periode.getPeriodeSøknad().isPresent()) {
            builder.medPeriodeSoknad(periode.getPeriodeSøknad().get());
        }

        var kopiertPeriode = builder.build();
        for (var aktivitet : periode.getAktiviteter()) {
            kopiertPeriode.leggTilAktivitet(opphørAktivitet(periode, aktivitet));
        }

        return kopiertPeriode;
    }

    private PeriodeResultatÅrsak opphørAvslagsårsak(Behandling behandling) {
        var vilkårResultat = vilkårResultatRepository.hent(behandling.getId());
        if (!opptjeningsvilkåretOppfylt(vilkårResultat)) {
            return PeriodeResultatÅrsak.OPPTJENINGSVILKÅRET_IKKE_OPPFYLT;
        }
        if (!fødselsvilkårOppfylt(vilkårResultat, BehandlingReferanse.fra(behandling))) {
            return PeriodeResultatÅrsak.FØDSELSVILKÅRET_IKKE_OPPFYLT;
        }
        if (!adopsjonsvilkåretOppfylt(vilkårResultat)) {
            return PeriodeResultatÅrsak.ADOPSJONSVILKÅRET_IKKE_OPPFYLT;
        }
        if (!foreldreansvarsvilkåretOppfylt(vilkårResultat)) {
            return PeriodeResultatÅrsak.FORELDREANSVARSVILKÅRET_IKKE_OPPFYLT;
        }
        throw new ForvaltningException("Alle inngangsvilkår er oppfylt");
    }

    private UttakResultatPeriodeAktivitetEntitet opphørAktivitet(UttakResultatPeriodeEntitet periode,
                                                                 UttakResultatPeriodeAktivitetEntitet aktivitet) {
        return new UttakResultatPeriodeAktivitetEntitet.Builder(periode, aktivitet.getUttakAktivitet()).medTrekkonto(
            aktivitet.getTrekkonto())
            .medTrekkdager(Trekkdager.ZERO)
            .medArbeidsprosent(aktivitet.getArbeidsprosent())
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medErSøktGradering(aktivitet.isSøktGradering())
            .build();
    }

    private UttakResultatEntitet hentForrigeUttaksresultat(Behandling behandling) {
        var originalBehandlingId = behandling.getOriginalBehandlingId().orElseThrow();
        return fpUttakRepository.hentUttakResultat(originalBehandlingId);
    }

    public void beregnKontoer(UUID behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var input = uttakInputTjeneste.lagInput(behandling);
        var fagsak = fagsakRepository.finnEksaktFagsak(input.getBehandlingReferanse().getFagsakId());
        fagsakRelasjonRepository.nullstillOverstyrtStønadskontoberegning(fagsak);
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);
    }

    public void endreAnnenForelderHarRett(UUID behandlingUUID, boolean harRett) {
        var behandlingId = behandlingRepository.hentBehandling(behandlingUUID).getId();
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingId);
        if (ytelseFordelingAggregat.getPerioderAnnenforelderHarRett().isEmpty()) {
            throw new ForvaltningException("Kan ikke endre ettersom annen forelder har rett ikke er avklart");
        }
        if (UttakOmsorgUtil.harAnnenForelderRett(ytelseFordelingAggregat, Optional.empty()) != harRett) {
            ytelseFordelingTjeneste.bekreftAnnenforelderHarRett(behandlingId, harRett);

            lagHistorikkinnslagRett(harRett, behandlingId);
        }
    }

    public void endreAleneomsorg(UUID behandlingUuid, Boolean aleneomsorg) {
        var behandlingId = behandlingRepository.hentBehandling(behandlingUuid).getId();
        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForOmsorg(behandlingId, new BekreftFaktaForOmsorgVurderingAksjonspunktDto(aleneomsorg,
            null, null));

        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        historikkinnslag.setBehandlingId(behandlingId);

        var begrunnelse = aleneomsorg ? "FORVALTNING - Endret til aleneomsorg" : "FORVALTNING - Endret til ikke aleneomsorg";
        var historieBuilder = new HistorikkInnslagTekstBuilder().medHendelse(HistorikkinnslagType.FAKTA_ENDRET)
            .medBegrunnelse(begrunnelse);
        historieBuilder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

    private void lagHistorikkinnslagRett(boolean harRett, Long behandlingId) {
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        historikkinnslag.setBehandlingId(behandlingId);

        var begrunnelse = harRett ? "FORVALTNING - Endret til annen forelder har rett" : "FORVALTNING - Endret til annen forelder har ikke rett";
        var historieBuilder = new HistorikkInnslagTekstBuilder().medHendelse(HistorikkinnslagType.FAKTA_ENDRET)
            .medBegrunnelse(begrunnelse);
        historieBuilder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }
}
