package no.nav.foreldrepenger.familiehendelse;

import static java.util.stream.Collectors.toList;
import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.FØDSEL;
import static no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType.TERMIN;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.threeten.extra.Interval;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.IntervallUtil;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamiliehendelseEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.foreldrepenger.domene.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.foreldrepenger.familiehendelse.event.FamiliehendelseEventPubliserer;

@ApplicationScoped
public class FamilieHendelseTjeneste {

    private static final int ANTALL_UKER_FOM_TERMIN_SØKNADSFRIST_START = 16;
    private static final int ANTALL_UKER_TOM_TERMIN_SØKNADSFRIST_SLUTT = 4;
    private static final Period REGISTRERING_FRIST_ETTER_TERMIN = Period.parse("P25D");
    private static final Period REGISTRERING_FRIST_ETTER_FØDSEL = Period.parse("P14D");

    private FamilieHendelseRepository familieGrunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private BasisPersonopplysningTjeneste personopplysningTjeneste;
    private FamiliehendelseEventPubliserer familiehendelseEventPubliserer;

    FamilieHendelseTjeneste() {
        // CDI
    }

    @Inject
    public FamilieHendelseTjeneste(BasisPersonopplysningTjeneste personopplysningTjeneste,
                                       FamiliehendelseEventPubliserer familiehendelseEventPubliserer,
                                       BehandlingRepositoryProvider repositoryProvider) {

        this.personopplysningTjeneste = personopplysningTjeneste;
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familiehendelseEventPubliserer = familiehendelseEventPubliserer;
    }


    public List<Interval> beregnGyldigeFødselsperioder(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        final FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregat(behandlingId);
        final FamilieHendelseEntitet søknadVersjon = familieHendelseGrunnlag.getSøknadVersjon();
        Optional<LocalDate> fødselsdato = søknadVersjon.getBarna().stream().map(UidentifisertBarn::getFødselsdato).findFirst();
        Optional<LocalDate> termindato = søknadVersjon.getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato);
        List<LocalDate> adopsjonFødselsdatoer = søknadVersjon.getBarna().stream()
            .map(UidentifisertBarn::getFødselsdato).collect(toList());

        if (fødselsdato.isPresent() && søknadVersjon.getType().equals(FamilieHendelseType.FØDSEL)) {
            return Collections.singletonList(IntervallUtil.byggIntervall(fødselsdato.get().minusDays(1), fødselsdato.get().plusDays(1)));
        }
        if (termindato.isPresent()) {
            return Collections.singletonList(IntervallUtil.byggIntervall(termindato.get().minusWeeks(ANTALL_UKER_FOM_TERMIN_SØKNADSFRIST_START),
                termindato.get().plusWeeks(ANTALL_UKER_TOM_TERMIN_SØKNADSFRIST_SLUTT)));
        }
        if (adopsjonFødselsdatoer != null && søknadVersjon.getType().equals(FamilieHendelseType.ADOPSJON)) {
            return adopsjonFødselsdatoer.stream()
                .map(dato -> IntervallUtil.byggIntervall(dato, dato))
                .collect(toList());
        }

        // Ikke mulig å beregne gyldig fødselsperiode; returner tom liste
        return Collections.emptyList();
    }


    public void oppdaterFødselPåGrunnlag(Behandling behandling, List<FødtBarnInfo> bekreftetTps) {
        if (bekreftetTps.isEmpty()) {
            return;
        }

        final FamilieHendelseBuilder hendelseBuilder = familieGrunnlagRepository.opprettBuilderFor(behandling)
            .tilbakestillBarn();

        bekreftetTps.forEach(barn -> hendelseBuilder.leggTilBarn(barn.getFødselsdato(), barn.getDødsdato().orElse(null)));
        hendelseBuilder.medAntallBarn(bekreftetTps.size());

        familieGrunnlagRepository.lagreRegisterHendelse(behandling, hendelseBuilder);

        final FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = hentAggregat(behandling.getId());
        if (TERMIN.equals(familieHendelseGrunnlag.getSøknadVersjon().getType()) &&
            familieHendelseGrunnlag.getBekreftetVersjon().map(FamilieHendelseEntitet::getType).map(FØDSEL::equals).orElse(Boolean.FALSE)) {
            familiehendelseEventPubliserer.fireEvent(FamiliehendelseEvent.EventType.TERMIN_TIL_FØDSEL,behandling);
        }
    }


    public FamilieHendelseBuilder opprettBuilderFor(Behandling behandling) {
        return familieGrunnlagRepository.opprettBuilderFor(behandling);
    }


    public void lagreOverstyrtHendelse(Behandling behandling, FamilieHendelseBuilder hendelse) {
        familieGrunnlagRepository.lagreOverstyrtHendelse(behandling, hendelse);

        final FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = hentAggregat(behandling.getId());
        if (TERMIN.equals(familieHendelseGrunnlag.getSøknadVersjon().getType()) &&
            familieHendelseGrunnlag.getOverstyrtVersjon().map(FamilieHendelseEntitet::getType).map(FØDSEL::equals).orElse(Boolean.FALSE) ){
            familiehendelseEventPubliserer.fireEvent(FamiliehendelseEvent.EventType.TERMIN_TIL_FØDSEL,behandling);
        }
    }


    public FamilieHendelseGrunnlagEntitet hentAggregat(BehandlingReferanse ref) {
        return familieGrunnlagRepository.hentAggregat(ref.getBehandlingId());
    }


    public FamilieHendelseGrunnlagEntitet hentAggregat(Long behandlingId) {
        return familieGrunnlagRepository.hentAggregat(behandlingId);
    }


    public Optional<FamilieHendelseGrunnlagEntitet> finnAggregat(Long behandlingId) {
        return familieGrunnlagRepository.hentAggregatHvisEksisterer(behandlingId);
    }


    public FamilieHendelseGrunnlagEntitet hentFamilieHendelserPåGrunnlagId(Long aggregatId) {
        return familieGrunnlagRepository.hentFamilieHendelserPåGrunnlagId(aggregatId);
    }


    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        Optional<Long> funnetId = familieGrunnlagRepository.hentIdPåAktivFamiliehendelse(behandlingId);
        return funnetId
            .map(id -> EndringsresultatSnapshot.medSnapshot(FamilieHendelseGrunnlagEntitet.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(FamilieHendelseGrunnlagEntitet.class));
    }


    public DiffResult diffResultat(EndringsresultatDiff idDiff, boolean kunSporedeEndringer) {
        FamilieHendelseGrunnlagEntitet grunnlag1 = familieGrunnlagRepository.hentFamilieHendelserPåGrunnlagId((Long) idDiff.getGrunnlagId1());
        FamilieHendelseGrunnlagEntitet grunnlag2 = familieGrunnlagRepository.hentFamilieHendelserPåGrunnlagId((Long) idDiff.getGrunnlagId2());
        return familieGrunnlagRepository.diffResultat(grunnlag1, grunnlag2, kunSporedeEndringer);
    }


    public Optional<Boolean> gjelderFødsel(Behandling behandling) {
        Optional<Behandling> behandlingOptional = behandling.erYtelseBehandling() ? Optional.of(behandling) : behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(behandling.getFagsakId());
        return behandlingOptional
            .flatMap(b -> familieGrunnlagRepository.hentAggregatHvisEksisterer(b.getId()))
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getGjelderFødsel);
    }

    public boolean harFagsakFamilieHendelseDato(LocalDate familieHendelseDato, Long avsluttetFagsakId) {
        Optional<LocalDate> dato2 = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(avsluttetFagsakId)
            .flatMap(b -> familieGrunnlagRepository.hentAggregatHvisEksisterer(b.getId()))
            .map(FamilieHendelseGrunnlagEntitet::finnGjeldendeFødselsdato);
        return dato2.isPresent() && familieHendelseDato.equals(dato2.get());
    }

    public List<PersonopplysningEntitet> finnBarnSøktStønadFor(BehandlingReferanse ref) {
        List<Interval> fødselsintervall = this.beregnGyldigeFødselsperioder(ref);
        PersonopplysningerAggregat personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);

        return personopplysninger.getRelasjoner().stream()
            .filter(rel -> rel.getAktørId().equals(ref.getAktørId()) && rel.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .map(rel -> personopplysninger.getPersonopplysninger().stream().filter(person -> person.getAktørId().equals(rel.getTilAktørId())).findAny().orElse(null))
            .filter(barn -> barn != null && erBarnRelatertTilSøknad(fødselsintervall, barn.getFødselsdato()))
            .collect(toList());
    }

    private boolean erBarnRelatertTilSøknad(List<Interval> relasjonsintervall, LocalDate dato) {
        return relasjonsintervall.stream()
            .anyMatch(periode -> periode.overlaps(IntervallUtil.tilIntervall(dato)));
    }


    public boolean getManglerFødselsRegistreringFristUtløpt(FamilieHendelseGrunnlagEntitet grunnlag) {
        if (!grunnlag.getGjeldendeVersjon().getGjelderFødsel() || grunnlag.getBekreftetVersjon().isPresent() ||
            grunnlag.getOverstyrtVersjon().map(fh -> FØDSEL.equals(fh.getType()) && !fh.getBarna().isEmpty()).orElse(Boolean.FALSE)) {
            return false;
        }
        LocalDate fhDato = grunnlag.finnGjeldendeFødselsdato();
        Period fhDatoPlussFrist = FamilieHendelseType.FØDSEL.equals(grunnlag.getGjeldendeVersjon().getType()) ? REGISTRERING_FRIST_ETTER_FØDSEL : REGISTRERING_FRIST_ETTER_TERMIN;
        return LocalDate.now().isAfter(fhDato.plus(fhDatoPlussFrist));
    }

    public void kopierGrunnlag(Long fraBehandlingId, Long tilBehandlingId) {
        familieGrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(fraBehandlingId, tilBehandlingId);
    }
}
