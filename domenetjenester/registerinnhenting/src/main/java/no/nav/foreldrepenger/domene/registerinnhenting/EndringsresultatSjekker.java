package no.nav.foreldrepenger.domene.registerinnhenting;

import java.util.Objects;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
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
    private StønadsperioderInnhenter stønadsperioderInnhenter;
    private MorsAktivitetInnhenter morsAktivitetInnhenter;

    @Inject
    public EndringsresultatSjekker(PersonopplysningTjeneste personopplysningTjeneste,
                                   FamilieHendelseTjeneste familieHendelseTjeneste,
                                   MedlemTjeneste medlemTjeneste,
                                   InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                   YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                   StønadsperioderInnhenter stønadsperioderInnhenter,
                                   MorsAktivitetInnhenter morsAktivitetInnhenter) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.medlemTjeneste = medlemTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.stønadsperioderInnhenter = stønadsperioderInnhenter;
        this.morsAktivitetInnhenter = morsAktivitetInnhenter;
    }

    EndringsresultatSjekker() {
        // For CDI
    }

    public EndringsresultatSnapshot opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(Long behandlingId) {
        var snapshot = EndringsresultatSnapshot.opprett();
        snapshot.leggTil(personopplysningTjeneste.finnAktivGrunnlagId(behandlingId));
        snapshot.leggTil(familieHendelseTjeneste.finnAktivGrunnlagId(behandlingId));
        snapshot.leggTil(medlemTjeneste.finnAktivGrunnlagId(behandlingId));

        var iaySnapshot = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId)
            .map(iayg -> EndringsresultatSnapshot.medSnapshot(InntektArbeidYtelseGrunnlag.class,
                iayg.getEksternReferanse()))
            .orElse(EndringsresultatSnapshot.utenSnapshot(InntektArbeidYtelseGrunnlag.class));

        snapshot.leggTil(iaySnapshot);
        snapshot.leggTil(ytelseFordelingTjeneste.finnAktivAggregatId(behandlingId));
        snapshot.leggTil(stønadsperioderInnhenter.finnAktivGrunnlagId(behandlingId));
        snapshot.leggTil(morsAktivitetInnhenter.finnAktivGrunnlagId(behandlingId));

        return snapshot;
    }

    public EndringsresultatDiff finnSporedeEndringerPåBehandlingsgrunnlag(Long behandlingId,
                                                                          EndringsresultatSnapshot idSnapshotFør) {
        // Del 1: Finn diff mellom grunnlagets id før og etter oppdatering
        var idSnapshotNå = opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(behandlingId);
        var idDiff = idSnapshotNå.minus(idSnapshotFør);

        // Del 2: Transformer diff på grunnlagets id til diff på grunnlagets sporede endringer (@ChangeTracked)
        var sporedeEndringerDiff = EndringsresultatDiff.opprettForSporingsendringer();
        idDiff.hentDelresultat(PersonInformasjonEntitet.class)
            .ifPresent(idEndring -> sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> diffResultatPersonopplysninger(idEndring)));
        idDiff.hentDelresultat(FamilieHendelseGrunnlagEntitet.class)
            .ifPresent(idEndring -> sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> diffResultatFamilieHendelse(idEndring)));
        idDiff.hentDelresultat(MedlemskapAggregat.class)
            .ifPresent(idEndring -> sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> diffResultatMedslemskap(idEndring)));
        idDiff.hentDelresultat(InntektArbeidYtelseGrunnlag.class)
            .ifPresent(idEndring -> sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> diffResultatIay(behandlingId, idEndring)));
        idDiff.hentDelresultat(YtelseFordelingAggregat.class)
            .ifPresent(idEndring -> sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> diffResultatYf(idEndring)));
        idDiff.hentDelresultat(NesteSakGrunnlagEntitet.class)
            .ifPresent(idEndring -> sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> diffResultatNesteSak(idEndring)));
        idDiff.hentDelresultat(AktivitetskravGrunnlagEntitet.class)
            .ifPresent(idEndring -> sporedeEndringerDiff.leggTilSporetEndring(idEndring, () -> diffResultatAktivitetskravArbeid(idEndring)));
        return sporedeEndringerDiff;
    }

    private DiffResult diffResultatPersonopplysninger(EndringsresultatDiff idDiff) {
        var grunnlag1 = personopplysningTjeneste.hentGrunnlagPåId((Long) idDiff.getGrunnlagId1());
        var grunnlag2 = personopplysningTjeneste.hentGrunnlagPåId((Long) idDiff.getGrunnlagId2());
        return new RegisterdataDiffsjekker(true).getDiffEntity().diff(grunnlag1, grunnlag2);
    }

    private DiffResult diffResultatFamilieHendelse(EndringsresultatDiff idDiff) {
        var grunnlag1 = familieHendelseTjeneste.hentGrunnlagPåId((Long) idDiff.getGrunnlagId1());
        var grunnlag2 = familieHendelseTjeneste.hentGrunnlagPåId((Long) idDiff.getGrunnlagId2());
        return new RegisterdataDiffsjekker(true).getDiffEntity().diff(grunnlag1, grunnlag2);
    }

    private DiffResult diffResultatMedslemskap(EndringsresultatDiff idDiff) {
        nullSjekk(idDiff);

        var grunnlag1 = medlemTjeneste.hentGrunnlagPåId((Long) idDiff.getGrunnlagId1())
            .orElseThrow(() -> new IllegalStateException("id1 ikke kjent"));
        var grunnlag2 = medlemTjeneste.hentGrunnlagPåId((Long) idDiff.getGrunnlagId2())
            .orElseThrow(() -> new IllegalStateException("id2 ikke kjent"));
        return new RegisterdataDiffsjekker(true).getDiffEntity().diff(grunnlag1, grunnlag2);
    }

    private DiffResult diffResultatIay(Long behandlingId, EndringsresultatDiff idEndring) {
        var grunnlag1 = inntektArbeidYtelseTjeneste.hentGrunnlagPåId(behandlingId,
            (UUID) idEndring.getGrunnlagId1());
        var grunnlag2 = inntektArbeidYtelseTjeneste.hentGrunnlagPåId(behandlingId,
            (UUID) idEndring.getGrunnlagId2());
        return new IAYGrunnlagDiff(grunnlag1, grunnlag2).diffResultat(true);
    }

    private DiffResult diffResultatYf(EndringsresultatDiff idDiff) {
        nullSjekk(idDiff);

        var grunnlag1 = ytelseFordelingTjeneste.hentGrunnlagPåId((Long) idDiff.getGrunnlagId1())
            .orElseThrow(() -> new IllegalStateException("GrunnlagId1 må være oppgitt"));
        var grunnlag2 = ytelseFordelingTjeneste.hentGrunnlagPåId((Long) idDiff.getGrunnlagId2())
            .orElseThrow(() -> new IllegalStateException("GrunnlagId2 må være oppgitt"));
        return new RegisterdataDiffsjekker(true).getDiffEntity().diff(grunnlag1, grunnlag2);
    }

    private DiffResult diffResultatNesteSak(EndringsresultatDiff idDiff) {
        nullSjekk(idDiff);

        var grunnlag1 = stønadsperioderInnhenter.hentGrunnlagPåId((Long) idDiff.getGrunnlagId1());
        var grunnlag2 = stønadsperioderInnhenter.hentGrunnlagPåId((Long) idDiff.getGrunnlagId2());
        return new RegisterdataDiffsjekker(true).getDiffEntity().diff(grunnlag1, grunnlag2);
    }

    private DiffResult diffResultatAktivitetskravArbeid(EndringsresultatDiff idDiff) {
        nullSjekk(idDiff);
        var grunnlag1 = stønadsperioderInnhenter.hentGrunnlagPåId((Long) idDiff.getGrunnlagId1());
        var grunnlag2 = stønadsperioderInnhenter.hentGrunnlagPåId((Long) idDiff.getGrunnlagId2());
        return new RegisterdataDiffsjekker(true).getDiffEntity().diff(grunnlag1, grunnlag2);
    }

    private void nullSjekk(EndringsresultatDiff idDiff) {
        Objects.requireNonNull(idDiff.getGrunnlagId1(), "kan ikke diffe når id1 ikke er oppgitt");
        Objects.requireNonNull(idDiff.getGrunnlagId2(), "kan ikke diffe når id2 ikke er oppgitt");
    }
}
