package no.nav.foreldrepenger.domene.registerinnhenting;

import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
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


    EndringsresultatSjekker() {
        // For CDI
    }

    @Inject
    public EndringsresultatSjekker(PersonopplysningTjeneste personopplysningTjeneste,
                                   FamilieHendelseTjeneste familieHendelseTjeneste,
                                   MedlemTjeneste medlemTjeneste,
                                   InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                   YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.medlemTjeneste = medlemTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
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

}
