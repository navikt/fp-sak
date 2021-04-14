package no.nav.foreldrepenger.skjæringstidspunkt.fp;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.SoekerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.FagsakÅrsak;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsPeriode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsperiodeGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp.RegelFastsettOpptjeningsperiode;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class SkjæringstidspunktUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SkjæringstidspunktUtils.class);

    private final Period grenseverdiFør;
    private final Period grenseverdiEtter;
    private final Period opptjeningsperiode;
    private final Period tidligsteUttakFørFødselPeriode;

    /**
     *
     * @param opptjeningsperiode - Opptjeningsperiode lengde før skjæringstidspunkt
     * @param tidligsteUttakFørFødselPeriode -
     * @param grenseverdiAvvikFør - Maks avvik før/etter STP for registerinnhenting før justering av perioden
     * @param grenseverdiAvvikEtter
     */
    @Inject
    public SkjæringstidspunktUtils(@KonfigVerdi(value = "fp.opptjeningsperiode.lengde", defaultVerdi = "P10M") Period opptjeningsperiode,
                                   @KonfigVerdi(value = "fp.uttak.tidligst.før.fødsel", defaultVerdi = "P12W") Period tidligsteUttakFørFødselPeriode,
                                   @KonfigVerdi(value = "fp.registerinnhenting.avvik.periode.før", defaultVerdi = "P4M") Period grenseverdiAvvikFør,
                                   @KonfigVerdi(value = "fp.registerinnhenting.avvik.periode.etter", defaultVerdi = "P1Y") Period grenseverdiAvvikEtter) {
        this.grenseverdiFør = grenseverdiAvvikFør;
        this.grenseverdiEtter = grenseverdiAvvikEtter;
        this.opptjeningsperiode = opptjeningsperiode;
        this.tidligsteUttakFørFødselPeriode = tidligsteUttakFørFødselPeriode;
    }

    public SkjæringstidspunktUtils() {
        this(Period.ofMonths(10), Period.ofWeeks(12), Period.ofMonths(4), Period.ofYears(1));
    }

    LocalDate utledSkjæringstidspunktRegisterinnhenting(FamilieHendelseGrunnlagEntitet familieHendelseAggregat) {
        final var gjeldendeHendelseDato = familieHendelseAggregat.getGjeldendeVersjon().getGjelderFødsel()
            ? familieHendelseAggregat.finnGjeldendeFødselsdato()
            : familieHendelseAggregat.getGjeldendeVersjon().getSkjæringstidspunkt();
        final var oppgittHendelseDato = familieHendelseAggregat.getSøknadVersjon().getSkjæringstidspunkt();

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
        return vurderEndringFør(oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt, grenseverdiFør)
            || vurderEndringEtter(oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt, grenseverdiEtter);
    }

    private boolean vurderEndringEtter(LocalDate oppgittSkjæringstidspunkt, LocalDate bekreftetSkjæringstidspunkt, Period grenseverdiEtter) {
        final var avstand = Period.between(oppgittSkjæringstidspunkt, bekreftetSkjæringstidspunkt);
        return !avstand.isNegative() && størreEnn(avstand, grenseverdiEtter);
    }

    private boolean vurderEndringFør(LocalDate oppgittSkjæringstidspunkt, LocalDate bekreftetSkjæringstidspunkt, Period grenseverdiFør) {
        final var avstand = Period.between(bekreftetSkjæringstidspunkt, oppgittSkjæringstidspunkt);
        return !avstand.isNegative() && størreEnn(avstand, grenseverdiFør);
    }

    private static boolean størreEnn(Period period, Period sammenligning) {
        return tilDager(period) > tilDager(sammenligning);
    }

    private static int tilDager(Period period) {
        return period.getDays() + (period.getMonths() * 30) + ((period.getYears() * 12) * 30);
    }

    LocalDate utledSkjæringstidspunktFraBehandling(Behandling behandling, LocalDate førsteUttaksDato,
                                                   Optional<FamilieHendelseGrunnlagEntitet> familieHendelseGrunnlag, Optional<LocalDate> morsMaksDato) {

        if (familieHendelseGrunnlag.isPresent()) {
            return evaluerSkjæringstidspunktOpptjening(behandling, førsteUttaksDato, familieHendelseGrunnlag.get(), morsMaksDato);
        }
        return førsteUttaksDato;
    }

    private LocalDate evaluerSkjæringstidspunktOpptjening(Behandling behandling, LocalDate førsteUttaksDato,
                                                          FamilieHendelseGrunnlagEntitet fhGrunnlag, Optional<LocalDate> morsMaksDato) {
        var grunnlag = new OpptjeningsperiodeGrunnlag();

        final var gjeldendeHendelseDato = fhGrunnlag.getGjeldendeVersjon().getGjelderFødsel() ? fhGrunnlag.finnGjeldendeFødselsdato()
            : fhGrunnlag.getGjeldendeVersjon().getSkjæringstidspunkt();


        grunnlag.setFagsakÅrsak(finnFagsakÅrsak(fhGrunnlag.getGjeldendeVersjon()));
        grunnlag.setSøkerRolle(finnFagsakSøkerRolle(behandling));
        if (grunnlag.getFagsakÅrsak() == null || grunnlag.getSøkerRolle() == null) {
            throw new IllegalArgumentException(
                "Utvikler-feil: Finner ikke årsak(" + grunnlag.getFagsakÅrsak() + ")/rolle(" + grunnlag.getSøkerRolle() + ") for behandling:" + behandling.getId());
        }

        grunnlag.setHendelsesDato(gjeldendeHendelseDato);
        fhGrunnlag.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).ifPresent(grunnlag::setTerminDato);

        if (grunnlag.getHendelsesDato() == null) {
            grunnlag.setHendelsesDato(grunnlag.getTerminDato());
            if (grunnlag.getHendelsesDato() == null) {
                throw new IllegalArgumentException("Utvikler-feil: Finner ikke hendelsesdato for behandling:" + behandling.getId());
            }
        }

        grunnlag.setTidligsteUttakFørFødselPeriode(tidligsteUttakFørFødselPeriode);
        grunnlag.setPeriodeLengde(opptjeningsperiode);
        grunnlag.setFørsteUttaksDato(førsteUttaksDato);
        morsMaksDato.ifPresent(grunnlag::setMorsMaksdato);

        final var fastsettPeriode = new RegelFastsettOpptjeningsperiode();
        final var periode = new OpptjeningsPeriode();
        fastsettPeriode.evaluer(grunnlag, periode);

        return periode.getOpptjeningsperiodeTom().plusDays(1);
    }

    // TODO(Termitt): Håndtere MMOR, SAMB mm.
    private SoekerRolle finnFagsakSøkerRolle(Behandling behandling) {
        var relasjonsRolleType = behandling.getRelasjonsRolleType();
        if (RelasjonsRolleType.MORA.equals(relasjonsRolleType)) {
            return SoekerRolle.MORA;
        }
        if (RelasjonsRolleType.UDEFINERT.equals(relasjonsRolleType) || RelasjonsRolleType.BARN.equals(relasjonsRolleType)) {
            return null;
        }
        return SoekerRolle.FARA;
    }

    private FagsakÅrsak finnFagsakÅrsak(FamilieHendelseEntitet gjeldendeVersjon) {
        final var type = gjeldendeVersjon.getType();
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
