package no.nav.foreldrepenger.domene.registerinnhenting;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

@Dependent
public class EndringsresultatSjekker {

    private PersonopplysningTjeneste personopplysningTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private MedlemTjeneste medlemTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    private OpptjeningRepository opptjeningRepository;
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private FpUttakRepository fpUttakRepository;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;

    EndringsresultatSjekker() {
        // For CDI
    }

    @Inject
    public EndringsresultatSjekker(PersonopplysningTjeneste personopplysningTjeneste,
                                   FamilieHendelseTjeneste familieHendelseTjeneste,
                                   MedlemTjeneste medlemTjeneste,
                                   InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                   YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                   HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                   BehandlingRepositoryProvider provider) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.medlemTjeneste = medlemTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.opptjeningRepository = provider.getOpptjeningRepository();
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.fpUttakRepository = provider.getFpUttakRepository();
        this.uttaksperiodegrenseRepository = provider.getUttaksperiodegrenseRepository();
    }

    public EndringsresultatSnapshot opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(Long behandlingId) {
        EndringsresultatSnapshot snapshot = EndringsresultatSnapshot.opprett();
        snapshot.leggTil(personopplysningTjeneste.finnAktivGrunnlagId(behandlingId));
        snapshot.leggTil(familieHendelseTjeneste.finnAktivGrunnlagId(behandlingId));
        snapshot.leggTil(medlemTjeneste.finnAktivGrunnlagId(behandlingId));

        EndringsresultatSnapshot iaySnapshot =  inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId)
                .map(iayg -> EndringsresultatSnapshot.medSnapshot(InntektArbeidYtelseGrunnlag.class, iayg.getEksternReferanse()))
                .orElse(EndringsresultatSnapshot.utenSnapshot(InntektArbeidYtelseGrunnlag.class));

        snapshot.leggTil(iaySnapshot);
        snapshot.leggTil(ytelseFordelingTjeneste.finnAktivAggregatId(behandlingId));

        return snapshot;
    }

    public EndringsresultatDiff finnSporedeEndringerPåBehandlingsgrunnlag(Long behandlingId, EndringsresultatSnapshot idSnapshotFør) {
        final boolean kunSporedeEndringer = true;
        // Del 1: Finn diff mellom grunnlagets id før og etter oppdatering
        EndringsresultatSnapshot idSnapshotNå = opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(behandlingId);
        EndringsresultatDiff idDiff = idSnapshotNå.minus(idSnapshotFør);

        // Del 2: Transformer diff på grunnlagets id til diff på grunnlagets sporede endringer (@ChangeTracked)
        EndringsresultatDiff sporedeEndringerDiff = EndringsresultatDiff.opprettForSporingsendringer();
        idDiff.hentDelresultat(PersonInformasjonEntitet.class).ifPresent(idEndring ->
            sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> personopplysningTjeneste.diffResultat(idEndring, kunSporedeEndringer)));
        idDiff.hentDelresultat(FamilieHendelseGrunnlagEntitet.class).ifPresent(idEndring ->
            sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> familieHendelseTjeneste.diffResultat(idEndring, kunSporedeEndringer)));
        idDiff.hentDelresultat(MedlemskapAggregat.class).ifPresent(idEndring ->
            sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> medlemTjeneste.diffResultat(idEndring, kunSporedeEndringer)));
        idDiff.hentDelresultat(InntektArbeidYtelseGrunnlag.class).ifPresent(idEndring ->
            sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> {
                InntektArbeidYtelseGrunnlag grunnlag1 = inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandlingId, (UUID)idEndring.getGrunnlagId1());
                InntektArbeidYtelseGrunnlag grunnlag2 = inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandlingId, (UUID)idEndring.getGrunnlagId2());
                return new IAYGrunnlagDiff(grunnlag1, grunnlag2).diffResultat(kunSporedeEndringer);
            }));
        idDiff.hentDelresultat(YtelseFordelingAggregat.class).ifPresent(idEndring ->
            sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> ytelseFordelingTjeneste.diffResultat(idEndring, kunSporedeEndringer)));
        return sporedeEndringerDiff;
    }

    public EndringsresultatSnapshot opprettEndringsresultatIdPåBehandlingSnapshot(Behandling behandling) {
        Long behandlingId = behandling.getId();
        EndringsresultatSnapshot snapshot = opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(behandlingId);

        snapshot.leggTil(opptjeningRepository.finnAktivGrunnlagId(behandling));
        snapshot.leggTil(finnAktivBeregningsgrunnlagGrunnlagAggregatId(behandlingId));
        snapshot.leggTil(fpUttakRepository.finnAktivAggregatId(behandling.getId()));
        snapshot.leggTil(uttaksperiodegrenseRepository.finnAktivAggregatId(behandling.getId()));

        // Resultatstrukturene nedenfor støtter ikke paradigme med "aktivt" grunnlag som kan identifisere med id
        // Aksepterer her at endringssjekk heller utledes av deres tidsstempel forutsatt at metoden ikke brukes i
        // kritiske endringssjekker. Håp om at de i fremtiden vil støtte paradigme.
        snapshot.leggTil(lagVilkårResultatIdSnapshotAvTidsstempel(behandling));
        snapshot.leggTil(lagBeregningResultatIdSnapshotAvTidsstempel(behandling));

        return snapshot;
    }

    public EndringsresultatDiff finnIdEndringerPåBehandling(Behandling behandling, EndringsresultatSnapshot idSnapshotFør) {
        EndringsresultatSnapshot idSnapshotNå = opprettEndringsresultatIdPåBehandlingSnapshot(behandling);
        return idSnapshotNå.minus(idSnapshotFør);
    }

    private EndringsresultatSnapshot lagVilkårResultatIdSnapshotAvTidsstempel(Behandling behandling) {
       return Optional.ofNullable(behandling.getBehandlingsresultat())
                 .map(Behandlingsresultat::getVilkårResultat)
                 .map(vilkårResultat ->
                     EndringsresultatSnapshot.medSnapshot(VilkårResultat.class, hentLongVerdiAvEndretTid(vilkårResultat)))
                 .orElse(EndringsresultatSnapshot.utenSnapshot(VilkårResultat.class));
    }

    private EndringsresultatSnapshot lagBeregningResultatIdSnapshotAvTidsstempel(Behandling behandling) {
       return Optional.ofNullable(behandling.getBehandlingsresultat())
            .map(Behandlingsresultat::getBeregningResultat)
            .map(beregningResultat ->
                EndringsresultatSnapshot.medSnapshot(LegacyESBeregningsresultat.class, hentLongVerdiAvEndretTid(beregningResultat)))
            .orElse(EndringsresultatSnapshot.utenSnapshot(LegacyESBeregningsresultat.class));
    }


    //Denne metoden bør legges i Tjeneste
    public EndringsresultatSnapshot finnAktivBeregningsgrunnlagGrunnlagAggregatId(Long behandlingId) {
        Optional<Long> aktivBeregningsgrunnlagGrunnlagId = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId).map(BeregningsgrunnlagGrunnlagEntitet::getId);
        return aktivBeregningsgrunnlagGrunnlagId
            .map(id -> EndringsresultatSnapshot.medSnapshot(BeregningsgrunnlagEntitet.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(BeregningsgrunnlagEntitet.class));
    }


    private Long hentLongVerdiAvEndretTid(BaseEntitet entitet) {
       LocalDateTime endretTidspunkt = entitet.getOpprettetTidspunkt();
       if(entitet.getEndretTidspunkt()!=null){
           endretTidspunkt = entitet.getEndretTidspunkt();
       }
       return mapFraLocalDateTimeTilLong(endretTidspunkt);
    }

    static Long mapFraLocalDateTimeTilLong(LocalDateTime ldt){
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("Europe/Paris"));
        return zdt.toInstant().toEpochMilli();
    }
}
