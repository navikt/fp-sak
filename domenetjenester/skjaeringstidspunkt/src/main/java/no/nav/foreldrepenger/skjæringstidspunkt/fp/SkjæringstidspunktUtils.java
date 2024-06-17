package no.nav.foreldrepenger.skjæringstidspunkt.fp;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
public class SkjæringstidspunktUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SkjæringstidspunktUtils.class);

    private final Period grenseverdiFør;
    private final Period grenseverdiEtter;

    /**
     * @param grenseverdiAvvikFør   - Maks avvik før/etter STP for registerinnhenting før justering av perioden
     * @param grenseverdiAvvikEtter
     */
    @Inject
    public SkjæringstidspunktUtils(@KonfigVerdi(value = "fp.registerinnhenting.avvik.periode.før", defaultVerdi = "P4M") Period grenseverdiAvvikFør,
                                   @KonfigVerdi(value = "fp.registerinnhenting.avvik.periode.etter", defaultVerdi = "P1Y") Period grenseverdiAvvikEtter) {
        this.grenseverdiFør = grenseverdiAvvikFør;
        this.grenseverdiEtter = grenseverdiAvvikEtter;
    }

    public SkjæringstidspunktUtils() {
        this(Period.ofMonths(4), Period.ofYears(1));
    }

    LocalDate utledSkjæringstidspunktRegisterinnhenting(FamilieHendelseGrunnlagEntitet familieHendelseAggregat) {
        var gjeldendeHendelseDato = familieHendelseAggregat.getGjeldendeVersjon()
            .getGjelderFødsel() ? familieHendelseAggregat.finnGjeldendeFødselsdato() : familieHendelseAggregat.getGjeldendeVersjon()
            .getSkjæringstidspunkt();
        var oppgittHendelseDato = familieHendelseAggregat.getSøknadVersjon().getSkjæringstidspunkt();

        if (erEndringIPerioden(oppgittHendelseDato, gjeldendeHendelseDato)) {
            LOG.info("STP registerinnhenting endring i perioden for fhgrunnlag {}", familieHendelseAggregat.getId());
            return gjeldendeHendelseDato;
        }
        return oppgittHendelseDato;
    }

    private boolean erEndringIPerioden(LocalDate oppgittSkjæringstidspunkt, LocalDate bekreftetSkjæringstidspunkt) {
        if (bekreftetSkjæringstidspunkt == null) {
            return false;
        }
        return vurderEndringFør(oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt, grenseverdiFør) || vurderEndringEtter(
            oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt, grenseverdiEtter);
    }

    private boolean vurderEndringEtter(LocalDate oppgittSkjæringstidspunkt, LocalDate bekreftetSkjæringstidspunkt, Period grenseverdiEtter) {
        var avstand = Period.between(oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt);
        return !avstand.isNegative() && størreEnn(avstand, grenseverdiEtter);
    }

    private boolean vurderEndringFør(LocalDate oppgittSkjæringstidspunkt, LocalDate bekreftetSkjæringstidspunkt, Period grenseverdiFør) {
        var avstand = Period.between(bekreftetSkjæringstidspunkt, oppgittSkjæringstidspunkt);
        return !avstand.isNegative() && størreEnn(avstand, grenseverdiFør);
    }

    private static boolean størreEnn(Period period, Period sammenligning) {
        return tilDager(period) > tilDager(sammenligning);
    }

    private static int tilDager(Period period) {
        return period.getDays() + period.getMonths() * 30 + period.getYears() * 12 * 30;
    }

    LocalDate utledSkjæringstidspunktFraBehandling(Behandling behandling,
                                                   LocalDate førsteUttaksDato,
                                                   Optional<FamilieHendelseGrunnlagEntitet> familieHendelseGrunnlag,
                                                   Optional<LocalDate> morsMaksDato,
                                                   boolean utenMinsterett) {

        return familieHendelseGrunnlag.map(g -> evaluerSkjæringstidspunktOpptjening(behandling, førsteUttaksDato, g, morsMaksDato, utenMinsterett))
            .orElse(førsteUttaksDato);
    }

    private LocalDate evaluerSkjæringstidspunktOpptjening(Behandling behandling,
                                                          LocalDate førsteUttaksDato,
                                                          FamilieHendelseGrunnlagEntitet fhGrunnlag,
                                                          Optional<LocalDate> morsMaksDato,
                                                          boolean utenMinsterett) {
        var gjeldendeHendelseDato = fhGrunnlag.getGjeldendeVersjon()
            .getGjelderFødsel() ? fhGrunnlag.finnGjeldendeFødselsdato() : fhGrunnlag.getGjeldendeVersjon().getSkjæringstidspunkt();
        var gjeldendeTermindato = fhGrunnlag.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato);

        var fagsakÅrsak = finnFagsakÅrsak(fhGrunnlag.getGjeldendeVersjon());
        var søkerRolle = finnFagsakSøkerRolle(behandling);
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
    private RegelSøkerRolle finnFagsakSøkerRolle(Behandling behandling) {
        var relasjonsRolleType = behandling.getRelasjonsRolleType();
        if (RelasjonsRolleType.MORA.equals(relasjonsRolleType)) {
            return RegelSøkerRolle.MORA;
        }
        if (RelasjonsRolleType.UDEFINERT.equals(relasjonsRolleType) || RelasjonsRolleType.BARN.equals(relasjonsRolleType)) {
            return null;
        }
        return RegelSøkerRolle.FARA;
    }

    private FagsakÅrsak finnFagsakÅrsak(FamilieHendelseEntitet gjeldendeVersjon) {
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
