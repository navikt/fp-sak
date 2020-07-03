package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.InngangsvilkårGrunnlagBygger.adopsjonsvilkåretOppfylt;
import static no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.InngangsvilkårGrunnlagBygger.foreldreansvarsvilkåretOppfylt;
import static no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.InngangsvilkårGrunnlagBygger.fødselsvilkårOppfylt;
import static no.nav.foreldrepenger.domene.uttak.fastsetteperioder.grunnlagbyggere.InngangsvilkårGrunnlagBygger.opptjeningsvilkåretOppfylt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.BeregnStønadskontoerTjeneste;

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

    @Inject
    public ForvaltningUttakTjeneste(FpUttakRepository fpUttakRepository,
                                    BehandlingRepository behandlingRepository,
                                    BehandlingsresultatRepository behandlingsresultatRepository,
                                    BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                    VilkårResultatRepository vilkårResultatRepository,
                                    BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste,
                                    UttakInputTjeneste uttakInputTjeneste,
                                    FagsakRelasjonRepository fagsakRelasjonRepository,
                                    FagsakRepository fagsakRepository) {
        this.fpUttakRepository = fpUttakRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.beregnStønadskontoerTjeneste = beregnStønadskontoerTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.fagsakRelasjonRepository = fagsakRelasjonRepository;
        this.fagsakRepository = fagsakRepository;
    }

    ForvaltningUttakTjeneste() {
        //CDI
    }

    boolean erFerdigForeldrepengerBehandlingSomHarFørtTilOpphør(long behandlingId) {
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return behandling.getStatus().erFerdigbehandletStatus()
            && FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())
            && behandlingsresultat.orElseThrow().isBehandlingsresultatOpphørt();
    }

    void lagOpphørtUttaksresultat(long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        kopierBeregningsgrunnlag(behandling);

        var forrigeUttaksresultat = hentForrigeUttaksresultat(behandling);
        var perioder = new UttakResultatPerioderEntitet();
        for (UttakResultatPeriodeEntitet uttaksperiode : forrigeUttaksresultat.getGjeldendePerioder().getPerioder()) {
            perioder.leggTilPeriode(opphørPeriode(behandling, uttaksperiode));
        }

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandlingId, perioder);
    }

    private void kopierBeregningsgrunnlag(Behandling behandling) {
        behandling.getOriginalBehandling().map(Behandling::getId)
            .ifPresent(originalId -> beregningsgrunnlagKopierOgLagreTjeneste.kopierBeregningsresultatFraOriginalBehandling(originalId, behandling.getId()));
    }

    private UttakResultatPeriodeEntitet opphørPeriode(Behandling behandling, UttakResultatPeriodeEntitet periode) {
        UttakResultatPeriodeEntitet.Builder builder = new UttakResultatPeriodeEntitet.Builder(periode.getFom(), periode.getTom())
            .medResultatType(PeriodeResultatType.AVSLÅTT, opphørAvslagsårsak(behandling))
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
        for (UttakResultatPeriodeAktivitetEntitet aktivitet : periode.getAktiviteter()) {
            kopiertPeriode.leggTilAktivitet(opphørAktivitet(periode, aktivitet));
        }

        return kopiertPeriode;
    }

    private IkkeOppfyltÅrsak opphørAvslagsårsak(Behandling behandling) {
        VilkårResultat vilkårResultat = vilkårResultatRepository.hent(behandling.getId());
        if (!opptjeningsvilkåretOppfylt(vilkårResultat)) {
            return IkkeOppfyltÅrsak.OPPTJENINGSVILKÅRET_IKKE_OPPFYLT;
        }
        if (!fødselsvilkårOppfylt(vilkårResultat, BehandlingReferanse.fra(behandling))) {
            return IkkeOppfyltÅrsak.FØDSELSVILKÅRET_IKKE_OPPFYLT;
        }
        if (!adopsjonsvilkåretOppfylt(vilkårResultat)) {
            return IkkeOppfyltÅrsak.ADOPSJONSVILKÅRET_IKKE_OPPFYLT;
        }
        if (!foreldreansvarsvilkåretOppfylt(vilkårResultat)) {
            return IkkeOppfyltÅrsak.FORELDREANSVARSVILKÅRET_IKKE_OPPFYLT;
        }
        throw new IllegalStateException("Alle inngangsvilkår er oppfylt");
    }

    private UttakResultatPeriodeAktivitetEntitet opphørAktivitet(UttakResultatPeriodeEntitet periode,
                                                                 UttakResultatPeriodeAktivitetEntitet aktivitet) {
        return new UttakResultatPeriodeAktivitetEntitet.Builder(periode, aktivitet.getUttakAktivitet())
            .medTrekkonto(aktivitet.getTrekkonto())
            .medTrekkdager(Trekkdager.ZERO)
            .medArbeidsprosent(aktivitet.getArbeidsprosent())
            .medUtbetalingsgrad(Utbetalingsgrad.ZERO)
            .medErSøktGradering(aktivitet.isSøktGradering())
            .build();
    }

    private UttakResultatEntitet hentForrigeUttaksresultat(Behandling behandling) {
        var originalBehandling = behandling.getOriginalBehandling().orElseThrow();
        return fpUttakRepository.hentUttakResultat(originalBehandling.getId());
    }

    public void beregnKontoer(long behandlingId) {
        var input = uttakInputTjeneste.lagInput(behandlingId);
        var fagsak = fagsakRepository.finnEksaktFagsak(input.getBehandlingReferanse().getFagsakId());
        fagsakRelasjonRepository.nullstillOverstyrtStønadskontoberegning(fagsak);
        beregnStønadskontoerTjeneste.opprettStønadskontoer(input);
    }
}
