package no.nav.foreldrepenger.skjæringstidspunkt.fp;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.FagsakÅrsak;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.LovVersjoner;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsPeriode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.OpptjeningsperiodeGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp.RegelFastsettOpptjeningsperiode;

public class SkjæringstidspunktUtils {

    private SkjæringstidspunktUtils() {
    }

    static LocalDate utledSkjæringstidspunktFraBehandling(Behandling behandling, LocalDate førsteUttaksDato,
                                                   Optional<FamilieHendelseGrunnlagEntitet> familieHendelseGrunnlag,
                                                   Optional<LocalDate> morsMaksDato, boolean utenMinsterett) {

        return familieHendelseGrunnlag
            .map(g -> evaluerSkjæringstidspunktOpptjening(behandling, førsteUttaksDato,g, morsMaksDato, utenMinsterett))
            .orElse(førsteUttaksDato);
    }

    private static LocalDate evaluerSkjæringstidspunktOpptjening(Behandling behandling, LocalDate førsteUttaksDato,
                                                          FamilieHendelseGrunnlagEntitet fhGrunnlag, Optional<LocalDate> morsMaksDato, boolean utenMinsterett) {
        var gjeldendeHendelseDato = fhGrunnlag.getGjeldendeVersjon()
            .getGjelderFødsel() ? fhGrunnlag.finnGjeldendeFødselsdato() : fhGrunnlag.getGjeldendeVersjon().getSkjæringstidspunkt();
        var gjeldendeTermindato = fhGrunnlag.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato);

        var fagsakÅrsak = finnFagsakÅrsak(fhGrunnlag.getGjeldendeVersjon());
        var søkerRolle =  finnFagsakSøkerRolle(behandling);
        if (fagsakÅrsak == null || søkerRolle == null) {
            throw new IllegalArgumentException("Utvikler-feil: Finner ikke årsak/rolle for behandling:" + behandling.getId());
        }

        var hendelsedato = Optional.ofNullable(gjeldendeHendelseDato)
            .or(() -> gjeldendeTermindato)
            .orElseThrow(() -> new IllegalArgumentException("Utvikler-feil: Finner ikke hendelsesdato for behandling:" + behandling.getId()));

        var grunnlag = OpptjeningsperiodeGrunnlag.grunnlag(fagsakÅrsak, søkerRolle, utenMinsterett ? LovVersjoner.KLASSISK : LovVersjoner.PROP15L2122)
            .medFørsteUttaksDato(førsteUttaksDato)
            .medHendelsesDato(hendelsedato)
            .medTerminDato(gjeldendeTermindato.orElse(null))
            .medMorsMaksdato(morsMaksDato.orElse(null));

        var fastsettPeriode = new RegelFastsettOpptjeningsperiode();
        var periode = new OpptjeningsPeriode();
        fastsettPeriode.evaluer(grunnlag, periode);

        return periode.getOpptjeningsperiodeTom().plusDays(1);
    }

    // TODO(Termitt): Håndtere MMOR, SAMB mm.
    private static RegelSøkerRolle finnFagsakSøkerRolle(Behandling behandling) {
        var relasjonsRolleType = behandling.getRelasjonsRolleType();
        if (RelasjonsRolleType.MORA.equals(relasjonsRolleType)) {
            return RegelSøkerRolle.MORA;
        }
        if (RelasjonsRolleType.UDEFINERT.equals(relasjonsRolleType) || RelasjonsRolleType.BARN.equals(relasjonsRolleType)) {
            return null;
        }
        return RegelSøkerRolle.FARA;
    }

    private static FagsakÅrsak finnFagsakÅrsak(FamilieHendelseEntitet gjeldendeVersjon) {
        var type = gjeldendeVersjon.getType();
        if (gjeldendeVersjon.getGjelderFødsel()) {
            return FagsakÅrsak.FØDSEL;
        }
        if (FamilieHendelseType.ADOPSJON.equals(type)) {
            return FagsakÅrsak.ADOPSJON;
        }
        if (FamilieHendelseType.OMSORG.equals(type)) {
            return FagsakÅrsak.OMSORG;
        }
        return null;
    }
}
