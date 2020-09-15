package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.bind.JAXBElement;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.Innsendingsvalg;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadVedleggEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilans;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittUtenlandskVirksomhet;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.MottattDokumentFeil;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.MottattDokumentOversetter;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.NamespaceRef;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.OppgittPeriodeMottattDatoTjeneste;
import no.nav.foreldrepenger.regler.uttak.felles.Virkedager;
import no.nav.foreldrepenger.søknad.v3.SøknadConstants;
import no.nav.vedtak.felles.xml.soeknad.endringssoeknad.v3.Endringssoeknad;
import no.nav.vedtak.felles.xml.soeknad.engangsstoenad.v3.Engangsstønad;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Adopsjon;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.AnnenForelder;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.AnnenForelderMedNorskIdent;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.AnnenForelderUtenNorskIdent;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Bruker;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Foedsel;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Medlemskap;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Omsorgsovertakelse;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.OppholdUtlandet;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Periode;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.SoekersRelasjonTilBarnet;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Termin;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Vedlegg;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Ytelse;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.AnnenOpptjening;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.EgenNaering;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Foreldrepenger;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Frilans;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.NorskOrganisasjon;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Opptjening;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Regnskapsfoerer;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.UtenlandskArbeidsforhold;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.UtenlandskOrganisasjon;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.AnnenOpptjeningTyper;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Dekningsgrader;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Innsendingstype;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Land;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Omsorgsovertakelseaarsaker;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Virksomhetstyper;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Arbeidsforhold;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Frilanser;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.SelvstendigNæringsdrivende;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Svangerskapspenger;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Tilrettelegging;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Gradering;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.LukketPeriodeMedVedlegg;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Oppholdsperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Overfoeringsperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Person;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Utsettelsesperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Uttaksperiode;

@NamespaceRef(SøknadConstants.NAMESPACE)
@ApplicationScoped
public class MottattDokumentOversetterSøknad implements MottattDokumentOversetter<MottattDokumentWrapperSøknad> { // NOSONAR - (essv)kan akseptere lang mapperklasse

    private VirksomhetTjeneste virksomhetTjeneste;
    private PersonopplysningRepository personopplysningRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private SøknadRepository søknadRepository;
    private MedlemskapRepository medlemskapRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private TpsTjeneste tpsTjeneste;
    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private DatavarehusTjeneste datavarehusTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private FagsakRepository fagsakRepository;
    private OppgittPeriodeMottattDatoTjeneste oppgittPeriodeMottattDatoTjeneste;

    @Inject
    public MottattDokumentOversetterSøknad(BehandlingRepositoryProvider repositoryProvider,
                                           VirksomhetTjeneste virksomhetTjeneste,
                                           InntektArbeidYtelseTjeneste iayTjeneste,
                                           TpsTjeneste tpsTjeneste,
                                           DatavarehusTjeneste datavarehusTjeneste,
                                           SvangerskapspengerRepository svangerskapspengerRepository,
                                           OppgittPeriodeMottattDatoTjeneste oppgittPeriodeMottattDatoTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.tpsTjeneste = tpsTjeneste;
        this.behandlingRevurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        this.datavarehusTjeneste = datavarehusTjeneste;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.oppgittPeriodeMottattDatoTjeneste = oppgittPeriodeMottattDatoTjeneste;
    }

    MottattDokumentOversetterSøknad() {
        // for CDI proxy
    }

    @Override
    public void trekkUtDataOgPersister(MottattDokumentWrapperSøknad wrapper, MottattDokument mottattDokument, Behandling behandling, Optional<LocalDate> gjelderFra) {
        if (wrapper.getOmYtelse() instanceof Endringssoeknad && !erEndring(mottattDokument)) { // NOSONAR - ok måte å finne riktig JAXB-type
            throw new IllegalArgumentException("Kan ikke sende inn en Endringssøknad uten å angi " + DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD + " samtidig. Fikk " + mottattDokument.getDokumentType());
        }

        if (erEndring(mottattDokument)) {
            persisterEndringssøknad(wrapper, mottattDokument, behandling, gjelderFra);
        } else {
            persisterSøknad(wrapper, mottattDokument, behandling);
            // DVH oppdatering skal normalt gå gjennom events - dette er en unntaksløsning for å sikre at DVH oppdateres med annen part (som er lagt på DVH-sak)
            datavarehusTjeneste.lagreNedFagsak(behandling.getFagsakId());
        }
    }

    private SøknadEntitet.Builder kopierSøknad(Behandling behandling) {
        SøknadEntitet.Builder søknadBuilder;
        Optional<Long> originalBehandlingIdOpt = behandling.getOriginalBehandlingId();
        if (originalBehandlingIdOpt.isPresent()) {
            Long behandlingId = behandling.getId();
            Long originalBehandlingId = originalBehandlingIdOpt.get();
            SøknadEntitet originalSøknad = søknadRepository.hentSøknad(originalBehandlingId);
            søknadBuilder = new SøknadEntitet.Builder(originalSøknad, false);

            personopplysningRepository.hentPersonopplysningerHvisEksisterer(originalBehandlingId).flatMap(PersonopplysningGrunnlagEntitet::getOppgittAnnenPart)
                .ifPresent(oap -> {
                    OppgittAnnenPartBuilder oppgittAnnenPartBuilder = new OppgittAnnenPartBuilder(oap);
                    personopplysningRepository.lagre(behandlingId, oppgittAnnenPartBuilder);
                });

            MedlemskapOppgittTilknytningEntitet oppgittTilknytning = medlemskapRepository.hentMedlemskap(behandlingId).flatMap(MedlemskapAggregat::getOppgittTilknytning).orElseThrow();
            MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder(oppgittTilknytning);
            medlemskapRepository.lagreOppgittTilkytning(behandlingId, oppgittTilknytningBuilder.build());
        } else {
            søknadBuilder = new SøknadEntitet.Builder();
        }

        return søknadBuilder;
    }

    private void persisterEndringssøknad(MottattDokumentWrapperSøknad wrapper,
                                         MottattDokument mottattDokument,
                                         Behandling behandling,
                                         Optional<LocalDate> gjelderFra) {
        LocalDate mottattDato = mottattDokument.getMottattDato();
        boolean elektroniskSøknad = mottattDokument.getElektroniskRegistrert();

        //Kopier og oppdater søknadsfelter.
        final SøknadEntitet.Builder søknadBuilder = kopierSøknad(behandling);
        byggFelleselementerForSøknad(søknadBuilder, wrapper, elektroniskSøknad, mottattDato, gjelderFra);
        List<Behandling> henlagteBehandlingerEtterInnvilget = behandlingRevurderingRepository.finnHenlagteBehandlingerEtterSisteInnvilgedeIkkeHenlagteBehandling(behandling.getFagsakId());
        if (!henlagteBehandlingerEtterInnvilget.isEmpty()) {
            søknadBuilder.medSøknadsdato(søknadRepository.hentSøknad(henlagteBehandlingerEtterInnvilget.get(0).getId()).getSøknadsdato());
        }

        if (wrapper.getOmYtelse() instanceof Endringssoeknad) { // NOSONAR
            final Endringssoeknad omYtelse = (Endringssoeknad) wrapper.getOmYtelse();
            byggYtelsesSpesifikkeFelterForEndringssøknad(omYtelse, behandling, gjelderFra.orElse(mottattDato));
        }
        søknadBuilder.medErEndringssøknad(true);
        final SøknadEntitet søknad = søknadBuilder.build();

        søknadRepository.lagreOgFlush(behandling, søknad);
    }

    private void persisterSøknad(MottattDokumentWrapperSøknad wrapper, MottattDokument mottattDokument, Behandling behandling) {
        LocalDate mottattDato = mottattDokument.getMottattDato();
        boolean elektroniskSøknad = mottattDokument.getElektroniskRegistrert();
        final FamilieHendelseBuilder hendelseBuilder = familieHendelseRepository.opprettBuilderFor(behandling);
        final SøknadEntitet.Builder søknadBuilder = new SøknadEntitet.Builder();
        byggFelleselementerForSøknad(søknadBuilder, wrapper, elektroniskSøknad, mottattDato, Optional.empty());
        Long behandlingId = behandling.getId();
        AktørId aktørId = behandling.getAktørId();
        if (wrapper.getOmYtelse() != null) {
            byggMedlemskap(wrapper, behandlingId, mottattDato);
        }
        if (skalByggeSøknadAnnenPart(wrapper)) {
            byggSøknadAnnenPart(wrapper, behandling);
        }

        byggYtelsesSpesifikkeFelter(wrapper, behandling, søknadBuilder);
        byggOpptjeningsspesifikkeFelter(wrapper, behandlingId);
        if (wrapper.getOmYtelse() instanceof Svangerskapspenger) {
            byggFamilieHendelseForSvangerskap((Svangerskapspenger) wrapper.getOmYtelse(), hendelseBuilder);
        } else {
            SoekersRelasjonTilBarnet soekersRelasjonTilBarnet = getSoekersRelasjonTilBarnet(wrapper);
            if (soekersRelasjonTilBarnet instanceof Foedsel) { // NOSONAR
                byggFødselsrelaterteFelter((Foedsel) soekersRelasjonTilBarnet, hendelseBuilder);
            } else if (soekersRelasjonTilBarnet instanceof Termin) { // NOSONAR
                byggTerminrelaterteFelter((Termin) soekersRelasjonTilBarnet, hendelseBuilder);
            } else if (soekersRelasjonTilBarnet instanceof Adopsjon) { // NOSONAR
                byggAdopsjonsrelaterteFelter((Adopsjon) soekersRelasjonTilBarnet, hendelseBuilder);
            } else if (soekersRelasjonTilBarnet instanceof Omsorgsovertakelse) {
                byggOmsorgsovertakelsesrelaterteFelter((Omsorgsovertakelse) soekersRelasjonTilBarnet, hendelseBuilder, søknadBuilder);
            }
        }
        familieHendelseRepository.lagre(behandling, hendelseBuilder);
        søknadBuilder.medErEndringssøknad(false);
        final RelasjonsRolleType relasjonsRolleType = utledRolle(wrapper.getBruker(), behandlingId, aktørId);
        final SøknadEntitet søknad = søknadBuilder
            .medRelasjonsRolleType(relasjonsRolleType).build();
        søknadRepository.lagreOgFlush(behandling, søknad);
        fagsakRepository.oppdaterRelasjonsRolle(behandling.getFagsakId(), søknad.getRelasjonsRolleType());
    }

    private void byggFamilieHendelseForSvangerskap(Svangerskapspenger omYtelse, FamilieHendelseBuilder hendelseBuilder) {
        LocalDate termindato = omYtelse.getTermindato();
        Objects.requireNonNull(termindato, "Termindato må være oppgitt");
        hendelseBuilder.medTerminbekreftelse(hendelseBuilder.getTerminbekreftelseBuilder()
            .medTermindato(termindato));
        LocalDate fødselsdato = omYtelse.getFødselsdato();
        if (fødselsdato != null) {
            hendelseBuilder.erFødsel().medFødselsDato(fødselsdato);
        }

    }

    private RelasjonsRolleType utledRolle(Bruker bruker, Long behandlingId, AktørId aktørId) {
        NavBrukerKjønn kjønn = tpsTjeneste.hentBrukerForAktør(aktørId)
            .map(Personinfo::getKjønn)
            .orElseThrow(() -> MottattDokumentFeil.FACTORY.dokumentManglerRelasjonsRolleType(behandlingId).toException());

        if (bruker == null || bruker.getSoeknadsrolle() == null) {
            return NavBrukerKjønn.MANN.equals(kjønn) ? RelasjonsRolleType.FARA : RelasjonsRolleType.MORA;
        }
        if (ForeldreType.MOR.getKode().equals(bruker.getSoeknadsrolle().getKode()) && erKvinne(kjønn)) {
            return RelasjonsRolleType.MORA;
        }
        if (ForeldreType.FAR.getKode().equals(bruker.getSoeknadsrolle().getKode()) && erMann(kjønn)) {
            return RelasjonsRolleType.FARA;
        }
        if (ForeldreType.MEDMOR.getKode().equals(bruker.getSoeknadsrolle().getKode()) && erKvinne(kjønn)) {
            return RelasjonsRolleType.MEDMOR;
        }
        // TODO: Mangler annen-omsorgsperson ..
        return NavBrukerKjønn.MANN.equals(kjønn) ? RelasjonsRolleType.FARA : RelasjonsRolleType.MORA;
    }

    private boolean erKvinne(NavBrukerKjønn kjønn) {
        return NavBrukerKjønn.KVINNE.equals(kjønn);
    }

    private boolean erMann(NavBrukerKjønn kjønn) {
        return NavBrukerKjønn.MANN.equals(kjønn);
    }

    private boolean erEndring(MottattDokument mottattDokument) {
        return mottattDokument.getDokumentType().equals(DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD);
    }

    private void byggYtelsesSpesifikkeFelterForEndringssøknad(Endringssoeknad omYtelse, Behandling behandling, LocalDate mottattDato) {
        final List<LukketPeriodeMedVedlegg> perioder = omYtelse.getFordeling().getPerioder();
        lagreFordeling(behandling, perioder, hentAnnenForelderErInformert(behandling), mottattDato);
    }

    private void byggYtelsesSpesifikkeFelter(MottattDokumentWrapperSøknad skjemaWrapper, Behandling behandling, SøknadEntitet.Builder søknadBuilder) {
        if (skjemaWrapper.getOmYtelse() instanceof Foreldrepenger) { // NOSONAR - ok måte å finne riktig JAXB-type
            final Foreldrepenger omYtelse = (Foreldrepenger) skjemaWrapper.getOmYtelse();

            Long behandlingId = behandling.getId();
            oversettOgLagreRettighet(behandlingId, omYtelse);
            oversettOgLagreDekningsgrad(behandlingId, omYtelse);
            oversettOgLagreFordeling(behandling, omYtelse, skjemaWrapper.getSkjema().getMottattDato());
        } else if (skjemaWrapper.getOmYtelse() instanceof Svangerskapspenger) {

            final Svangerskapspenger svangerskapspenger = (Svangerskapspenger) skjemaWrapper.getOmYtelse();
            oversettOgLagreTilrettelegging(svangerskapspenger, søknadBuilder, behandling);
        }
    }

    private void oversettOgLagreTilrettelegging(Svangerskapspenger svangerskapspenger, SøknadEntitet.Builder søknadBuilder, Behandling behandling) {

        SvpGrunnlagEntitet.Builder svpBuilder = new SvpGrunnlagEntitet.Builder().medBehandlingId(behandling.getId());
        List<SvpTilretteleggingEntitet> tilrettelegginger = new ArrayList<>();

        List<Tilrettelegging> tilretteleggingListe = svangerskapspenger.getTilretteleggingListe().getTilrettelegging();

        for (Tilrettelegging tilrettelegging : tilretteleggingListe) {

            SvpTilretteleggingEntitet.Builder builder = new SvpTilretteleggingEntitet.Builder();
            builder.medBehovForTilretteleggingFom(tilrettelegging.getBehovForTilretteleggingFom())
                .medKopiertFraTidligereBehandling(false)
                .medMottattTidspunkt(behandling.getOpprettetTidspunkt());

            if (tilrettelegging.getHelTilrettelegging() != null) {
                tilrettelegging.getHelTilrettelegging().forEach(helTilrettelegging -> builder.medHelTilrettelegging(helTilrettelegging.getTilrettelagtArbeidFom()));
            }
            if (tilrettelegging.getDelvisTilrettelegging() != null) {
                tilrettelegging.getDelvisTilrettelegging().forEach(delvisTilrettelegging -> builder.medDelvisTilrettelegging(delvisTilrettelegging.getTilrettelagtArbeidFom(), delvisTilrettelegging.getStillingsprosent()));
            }
            if (tilrettelegging.getIngenTilrettelegging() != null) {
                tilrettelegging.getIngenTilrettelegging().forEach(ingenTilrettelegging -> builder.medIngenTilrettelegging(ingenTilrettelegging.getSlutteArbeidFom()));
            }

            for (JAXBElement<Object> element : tilrettelegging.getVedlegg()) {
                Vedlegg vedlegg = (Vedlegg) element.getValue();
                SøknadVedleggEntitet.Builder vedleggBuilder = new SøknadVedleggEntitet.Builder()
                    .medErPåkrevdISøknadsdialog(true)
                    .medInnsendingsvalg(tolkInnsendingsvalg(vedlegg.getInnsendingstype()))
                    .medSkjemanummer(vedlegg.getSkjemanummer())
                    .medTilleggsinfo(vedlegg.getTilleggsinformasjon());
                søknadBuilder.leggTilVedlegg(vedleggBuilder.build());
            }

            oversettArbeidsforhold(builder, tilrettelegging.getArbeidsforhold());
            tilrettelegginger.add(builder.build());
        }

        Optional<SvpGrunnlagEntitet> eksisterendeGrunnlag = svangerskapspengerRepository.hentGrunnlag(behandling.getId());
        eksisterendeGrunnlag.ifPresent(eg -> {
            List<SvpTilretteleggingEntitet> gamle = eg.getOpprinneligeTilrettelegginger() != null ?
                eg.getOpprinneligeTilrettelegginger().getTilretteleggingListe() : Collections.emptyList();
            gamle.stream()
                .filter(tlg -> tilrettelegginger.stream().noneMatch(tlg2 -> gjelderSammeArbeidsforhold(tlg, tlg2)))
                .forEach(tilrettelegging -> {
                    var builder = new SvpTilretteleggingEntitet.Builder(tilrettelegging);
                    builder.medKopiertFraTidligereBehandling(true);
                    tilrettelegginger.add(builder.build());
                });
        });

        SvpGrunnlagEntitet svpGrunnlag = svpBuilder.medOpprinneligeTilrettelegginger(tilrettelegginger).build();
        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
    }

    private boolean gjelderSammeArbeidsforhold(SvpTilretteleggingEntitet tilrettelegging1, SvpTilretteleggingEntitet tilrettelegging2) {
        if (tilrettelegging1.getArbeidsgiver().isPresent() && tilrettelegging2.getArbeidsgiver().isPresent()) {
            return Objects.equals(tilrettelegging1.getArbeidsgiver(), tilrettelegging2.getArbeidsgiver());
        }
        if (ArbeidType.FRILANSER.equals(tilrettelegging1.getArbeidType()) && ArbeidType.FRILANSER.equals(tilrettelegging2.getArbeidType())) {
            return true;
        }
        return ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE.equals(tilrettelegging1.getArbeidType()) && ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE.equals(tilrettelegging2.getArbeidType());
    }

    private void oversettArbeidsforhold(SvpTilretteleggingEntitet.Builder builder, Arbeidsforhold arbeidsforhold) {

        if (arbeidsforhold instanceof no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Arbeidsgiver) { //NOSONAR: ok å bruke instanceof ved xml parsing
            builder.medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
            no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver arbeidsgiver;

            if (arbeidsforhold instanceof no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Virksomhet) {
                String orgnr = ((no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Virksomhet) arbeidsforhold).getIdentifikator();
                virksomhetTjeneste.hentOrganisasjon(orgnr);
                arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
            } else {
                PersonIdent arbeidsgiverIdent = new PersonIdent(((no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Arbeidsgiver) arbeidsforhold).getIdentifikator());
                Optional<AktørId> aktørIdArbeidsgiver = tpsTjeneste.hentAktørForFnr(arbeidsgiverIdent);
                if (!aktørIdArbeidsgiver.isPresent()) {
                    throw MottattDokumentFeil.FACTORY.finnerIkkeArbeidsgiverITPS().toException();
                }
                arbeidsgiver = Arbeidsgiver.person(aktørIdArbeidsgiver.get());
            }
            builder.medArbeidsgiver(arbeidsgiver);
        } else if (arbeidsforhold instanceof Frilanser) {
            builder.medArbeidType(ArbeidType.FRILANSER);
            builder.medOpplysningerOmRisikofaktorer(((Frilanser) arbeidsforhold).getOpplysningerOmRisikofaktorer());
            builder.medOpplysningerOmTilretteleggingstiltak(((Frilanser) arbeidsforhold).getOpplysningerOmTilretteleggingstiltak());
        } else if (arbeidsforhold instanceof SelvstendigNæringsdrivende) {
            builder.medArbeidType(ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE);
            builder.medOpplysningerOmTilretteleggingstiltak(((SelvstendigNæringsdrivende) arbeidsforhold).getOpplysningerOmTilretteleggingstiltak());
            builder.medOpplysningerOmRisikofaktorer(((SelvstendigNæringsdrivende) arbeidsforhold).getOpplysningerOmRisikofaktorer());
        } else {
            throw MottattDokumentFeil.FACTORY.ukjentArbeidsForholdType().toException();
        }
    }

    private void byggOpptjeningsspesifikkeFelter(MottattDokumentWrapperSøknad skjemaWrapper, Long behandlingId) {
        Optional<InntektArbeidYtelseGrunnlag> iayGrunnlag = iayTjeneste.finnGrunnlag(behandlingId);
        if (iayGrunnlag.isPresent() && iayGrunnlag.get().getOppgittOpptjening().isPresent()) {
            // TFP-1671: Abakus støtter ikke at oppgitt opptjening endres
            return;
        }

        Opptjening opptjening = null;
        if (skjemaWrapper.getOmYtelse() instanceof Foreldrepenger) { // NOSONAR - ok måte å finne riktig JAXB-type
            final Foreldrepenger omYtelse = (Foreldrepenger) skjemaWrapper.getOmYtelse();
            opptjening = omYtelse.getOpptjening();
        } else if (skjemaWrapper.getOmYtelse() instanceof Svangerskapspenger) {
            final Svangerskapspenger omYtelse = (Svangerskapspenger) skjemaWrapper.getOmYtelse();
            opptjening = omYtelse.getOpptjening();
        }

        if (opptjening != null && (!opptjening.getUtenlandskArbeidsforhold().isEmpty() || !opptjening.getAnnenOpptjening().isEmpty() || !opptjening.getEgenNaering().isEmpty() || nonNull(opptjening.getFrilans()))) {
            iayTjeneste.lagreOppgittOpptjening(behandlingId, mapOppgittOpptjening(opptjening));
        }
    }


    private void oversettOgLagreRettighet(Long behandlingId, Foreldrepenger omYtelse) {
        if (!isNull(omYtelse.getRettigheter())) {
            final OppgittRettighetEntitet oppgittRettighet = new OppgittRettighetEntitet(omYtelse.getRettigheter().isHarAnnenForelderRett(),
                omYtelse.getRettigheter().isHarOmsorgForBarnetIPeriodene(), omYtelse.getRettigheter().isHarAleneomsorgForBarnet());
            ytelsesFordelingRepository.lagre(behandlingId, oppgittRettighet);
        }
    }

    private void oversettOgLagreFordeling(Behandling behandling, Foreldrepenger omYtelse, LocalDate mottattDato) {
        final List<LukketPeriodeMedVedlegg> perioder = new ArrayList<>(omYtelse.getFordeling().getPerioder());
        boolean annenForelderErInformert = omYtelse.getFordeling().isAnnenForelderErInformert();
        lagreFordeling(behandling, perioder, annenForelderErInformert, mottattDato);
    }

    private void lagreFordeling(Behandling behandling,
                                List<LukketPeriodeMedVedlegg> perioder,
                                boolean annenForelderErInformert,
                                LocalDate mottattDato) {
        final List<OppgittPeriodeEntitet> oppgittPerioder = new ArrayList<>();
        for (LukketPeriodeMedVedlegg lukketPeriode : perioder) {
            var oppgittPeriode = oversettPeriode(lukketPeriode, mottattDato, behandling);
            oppgittPerioder.add(oppgittPeriode);
        }
        if (!inneholderVirkedager(oppgittPerioder)) {
            throw new IllegalArgumentException("Fordelingen må inneholde perioder med minst en virkedag");
        }
        ytelsesFordelingRepository.lagre(behandling.getId(), new OppgittFordelingEntitet(oppgittPerioder, annenForelderErInformert));
    }

    private boolean inneholderVirkedager(List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream().anyMatch(p -> Virkedager.beregnAntallVirkedager(p.getFom(), p.getTom()) > 0);
    }

    private boolean hentAnnenForelderErInformert(Behandling behandling) {
        //Papirsøknad frontend støtter ikke å sette annenForelderErInformert. Kopierer fra førstegangsbehandling
        Long originalBehandlingId = behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalArgumentException("Utviklerfeil: Endringssøknad må ha original behandling"));
        return ytelsesFordelingRepository.hentAggregat(originalBehandlingId).getOppgittFordeling().getErAnnenForelderInformert();
    }

    private void oversettOgLagreDekningsgrad(Long behandlingId, Foreldrepenger omYtelse) {
        final Dekningsgrader dekingsgrad = omYtelse.getDekningsgrad().getDekningsgrad();
        if (Integer.toString(OppgittDekningsgradEntitet.ÅTTI_PROSENT).equalsIgnoreCase(dekingsgrad.getKode())) {
            ytelsesFordelingRepository.lagre(behandlingId, OppgittDekningsgradEntitet.bruk80());
        } else if (Integer.toString(OppgittDekningsgradEntitet.HUNDRE_PROSENT).equalsIgnoreCase(dekingsgrad.getKode())) {
            ytelsesFordelingRepository.lagre(behandlingId, OppgittDekningsgradEntitet.bruk100());
        }
    }

    private OppgittPeriodeEntitet oversettPeriode(LukketPeriodeMedVedlegg lukketPeriode,
                                                  LocalDate mottattDatoFraSøknad,
                                                  Behandling behandling) {
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny()
            .medPeriode(lukketPeriode.getFom(), lukketPeriode.getTom());
        if (lukketPeriode instanceof Uttaksperiode) { // NOSONAR
            final Uttaksperiode periode = (Uttaksperiode) lukketPeriode;
            oversettUttakperiode(oppgittPeriodeBuilder, periode);
        } else if (lukketPeriode instanceof Oppholdsperiode) { // NOSONAR
            oppgittPeriodeBuilder.medÅrsak(OppholdÅrsak.fraKode(((Oppholdsperiode) lukketPeriode).getAarsak().getKode()));
            oppgittPeriodeBuilder.medPeriodeType(UttakPeriodeType.fraKode(UttakPeriodeType.ANNET.getKode()));
        } else if (lukketPeriode instanceof Overfoeringsperiode) { // NOSONAR
            oppgittPeriodeBuilder.medÅrsak(OverføringÅrsak.fraKode(((Overfoeringsperiode) lukketPeriode).getAarsak().getKode()));
            oppgittPeriodeBuilder.medPeriodeType(UttakPeriodeType.fraKode(((Overfoeringsperiode) lukketPeriode).getOverfoeringAv().getKode()));
        } else if (lukketPeriode instanceof Utsettelsesperiode) { // NOSONAR
            Utsettelsesperiode utsettelsesperiode = (Utsettelsesperiode) lukketPeriode;
            oversettUtsettelsesperiode(oppgittPeriodeBuilder, utsettelsesperiode);
        } else { // NOSONAR
            throw new IllegalStateException("Ukjent periodetype.");
        }
        var oppgittPeriode = oppgittPeriodeBuilder.build();
        var eksisterendeMottattDato = oppgittPeriodeMottattDatoTjeneste.finnMottattDatoForPeriode(behandling, oppgittPeriode);
        oppgittPeriode.setMottattDato(eksisterendeMottattDato.isPresent() ? eksisterendeMottattDato.get() : mottattDatoFraSøknad);
        return oppgittPeriode;
    }

    private void oversettUtsettelsesperiode(OppgittPeriodeBuilder oppgittPeriodeBuilder, Utsettelsesperiode utsettelsesperiode) {
        oppgittPeriodeBuilder.medErArbeidstaker(utsettelsesperiode.isErArbeidstaker());
        if (utsettelsesperiode.getUtsettelseAv() != null) {
            oppgittPeriodeBuilder.medPeriodeType(UttakPeriodeType.fraKode(utsettelsesperiode.getUtsettelseAv().getKode()));
        }
        oppgittPeriodeBuilder.medÅrsak(UtsettelseÅrsak.fraKode(utsettelsesperiode.getAarsak().getKode()));
        if (utsettelsesperiode.getMorsAktivitetIPerioden() != null) {
            oppgittPeriodeBuilder.medMorsAktivitet(MorsAktivitet.fraKode(utsettelsesperiode.getMorsAktivitetIPerioden().getKode()));
        }
    }

    private void oversettUttakperiode(OppgittPeriodeBuilder oppgittPeriodeBuilder, Uttaksperiode periode) {
        oppgittPeriodeBuilder.medPeriodeType(UttakPeriodeType.fraKode(periode.getType().getKode()));
        if (periode.isOenskerFlerbarnsdager() != null) {
            oppgittPeriodeBuilder.medFlerbarnsdager(periode.isOenskerFlerbarnsdager());
        }
        //Støtter nå enten samtidig uttak eller gradering. Mulig dette endres senere
        if (erSamtidigUttak(periode)) {
            oppgittPeriodeBuilder.medSamtidigUttak(true);
            oppgittPeriodeBuilder.medSamtidigUttaksprosent(new SamtidigUttaksprosent(periode.getSamtidigUttakProsent()));
        } else if (periode instanceof Gradering) {
            oversettGradering(oppgittPeriodeBuilder, (Gradering) periode);
        }
        if (periode.getMorsAktivitetIPerioden() != null && !periode.getMorsAktivitetIPerioden().getKode().isEmpty()) {
            oppgittPeriodeBuilder.medMorsAktivitet(MorsAktivitet.fraKode(periode.getMorsAktivitetIPerioden().getKode()));
        }
    }

    private boolean erSamtidigUttak(Uttaksperiode periode) {
        return periode.isOenskerSamtidigUttak() != null && periode.isOenskerSamtidigUttak();
    }

    private void oversettGradering(OppgittPeriodeBuilder oppgittPeriodeBuilder, Gradering gradering) {
        no.nav.vedtak.felles.xml.soeknad.uttak.v3.Arbeidsgiver arbeidsgiverFraSøknad = gradering.getArbeidsgiver();
        if (arbeidsgiverFraSøknad != null) {
            no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver arbeidsgiver = oversettArbeidsgiver(arbeidsgiverFraSøknad);
            oppgittPeriodeBuilder.medArbeidsgiver(arbeidsgiver);
        }

        if (!gradering.isErArbeidstaker() && !gradering.isErFrilanser() && !gradering.isErSelvstNæringsdrivende()) {
            throw new IllegalArgumentException("Graderingsperioder må enten ha valgt at/fl/sn");
        }

        oppgittPeriodeBuilder.medErArbeidstaker(gradering.isErArbeidstaker());
        oppgittPeriodeBuilder.medErFrilanser(gradering.isErFrilanser());
        oppgittPeriodeBuilder.medErSelvstendig(gradering.isErSelvstNæringsdrivende());
        oppgittPeriodeBuilder.medArbeidsprosent(BigDecimal.valueOf(gradering.getArbeidtidProsent()));
    }

    private no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver oversettArbeidsgiver(no.nav.vedtak.felles.xml.soeknad.uttak.v3.Arbeidsgiver arbeidsgiverFraSøknad) {
        if (arbeidsgiverFraSøknad instanceof Person) { // NOSONAR
            Optional<AktørId> aktørId = tpsTjeneste.hentAktørForFnr(PersonIdent.fra(arbeidsgiverFraSøknad.getIdentifikator()));
            if (!aktørId.isPresent()) {
                throw new IllegalStateException("Finner ikke arbeidsgiver");
            }
            return Arbeidsgiver.person(aktørId.get());
        } else if (arbeidsgiverFraSøknad instanceof no.nav.vedtak.felles.xml.soeknad.uttak.v3.Virksomhet) { // NOSONAR
            String orgnr = arbeidsgiverFraSøknad.getIdentifikator();
            virksomhetTjeneste.hentOrganisasjon(orgnr);
            return Arbeidsgiver.virksomhet(orgnr);
        } else {
            throw new IllegalStateException("Ukjent arbeidsgiver type " + arbeidsgiverFraSøknad.getClass());
        }
    }

    private OppgittOpptjeningBuilder mapOppgittOpptjening(Opptjening opptjening) {
        OppgittOpptjeningBuilder builder = OppgittOpptjeningBuilder.ny();
        opptjening.getAnnenOpptjening().forEach(annenOpptjening -> builder.leggTilAnnenAktivitet(mapAnnenAktivitet(annenOpptjening)));
        opptjening.getEgenNaering().forEach(egenNaering -> builder.leggTilEgneNæringer(mapEgenNæring(egenNaering)));
        opptjening.getUtenlandskArbeidsforhold().forEach(arbeidsforhold -> builder.leggTilOppgittArbeidsforhold(mapOppgittUtenlandskArbeidsforhold(arbeidsforhold)));
        if (nonNull(opptjening.getFrilans())) {
            opptjening.getFrilans().getPeriode().forEach(periode -> builder.leggTilAnnenAktivitet(mapFrilansPeriode(periode)));
            builder.leggTilFrilansOpplysninger(mapFrilansOpplysninger(opptjening.getFrilans()));
        }
        return builder;
    }

    private OppgittFrilans mapFrilansOpplysninger(Frilans frilans) {
        OppgittFrilans frilansEntitet = new OppgittFrilans();
        frilansEntitet.setErNyoppstartet(frilans.isErNyoppstartet());
        frilansEntitet.setHarInntektFraFosterhjem(frilans.isHarInntektFraFosterhjem());
        frilansEntitet.setHarNærRelasjon(frilans.isNaerRelasjon());
        frilansEntitet.setFrilansoppdrag(frilans.getFrilansoppdrag()
            .stream()
            .map(fo -> {
                OppgittFrilansoppdrag frilansoppdragEntitet = new OppgittFrilansoppdrag(fo.getOppdragsgiver(), mapPeriode(fo.getPeriode()));
                frilansoppdragEntitet.setFrilans(frilansEntitet);
                return frilansoppdragEntitet;
            }).collect(Collectors.toList()));
        return frilansEntitet;
    }

    private OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder mapOppgittUtenlandskArbeidsforhold(UtenlandskArbeidsforhold utenlandskArbeidsforhold) {
        OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder builder = OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny();
        Landkoder landkode = finnLandkode(utenlandskArbeidsforhold.getArbeidsland().getKode());
        builder.medUtenlandskVirksomhet(new OppgittUtenlandskVirksomhet(landkode, utenlandskArbeidsforhold.getArbeidsgiversnavn()));
        builder.medErUtenlandskInntekt(true);
        builder.medArbeidType(ArbeidType.UTENLANDSK_ARBEIDSFORHOLD);

        DatoIntervallEntitet periode = mapPeriode(utenlandskArbeidsforhold.getPeriode());
        builder.medPeriode(periode);
        return builder;
    }

    private OppgittAnnenAktivitet mapFrilansPeriode(Periode periode) {
        DatoIntervallEntitet datoIntervallEntitet = mapPeriode(periode);
        return new OppgittAnnenAktivitet(datoIntervallEntitet, ArbeidType.FRILANSER);
    }

    private OppgittAnnenAktivitet mapAnnenAktivitet(AnnenOpptjening annenOpptjening) {
        DatoIntervallEntitet datoIntervallEntitet = mapPeriode(annenOpptjening.getPeriode());
        AnnenOpptjeningTyper type = annenOpptjening.getType();

        ArbeidType arbeidType = ArbeidType.fraKode(type.getKode());
        return new OppgittAnnenAktivitet(datoIntervallEntitet, arbeidType);
    }

    private List<OppgittOpptjeningBuilder.EgenNæringBuilder> mapEgenNæring(EgenNaering egenNæring) {
        List<OppgittOpptjeningBuilder.EgenNæringBuilder> builders = new ArrayList<>();
        egenNæring.getVirksomhetstype().forEach(virksomhettype -> builders.add(mapEgenNæringForType(egenNæring, virksomhettype)));
        return builders;
    }

    private OppgittOpptjeningBuilder.EgenNæringBuilder mapEgenNæringForType(EgenNaering egenNæring, Virksomhetstyper virksomhettype) {
        OppgittOpptjeningBuilder.EgenNæringBuilder egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny();
        if (egenNæring instanceof NorskOrganisasjon) {
            NorskOrganisasjon norskOrganisasjon = (NorskOrganisasjon) egenNæring;
            String orgNr = norskOrganisasjon.getOrganisasjonsnummer();
            virksomhetTjeneste.hentOrganisasjon(orgNr);
            egenNæringBuilder.medVirksomhet(orgNr);
        } else {
            UtenlandskOrganisasjon utenlandskOrganisasjon = (UtenlandskOrganisasjon) egenNæring;
            Landkoder landkode = finnLandkode(utenlandskOrganisasjon.getRegistrertILand().getKode());
            egenNæringBuilder.medUtenlandskVirksomhet(new OppgittUtenlandskVirksomhet(landkode, utenlandskOrganisasjon.getNavn()));
        }

        // felles
        VirksomhetType virksomhetType = VirksomhetType.fraKode(virksomhettype.getKode());
        egenNæringBuilder.medPeriode(mapPeriode(egenNæring.getPeriode()))
            .medVirksomhetType(virksomhetType);

        Optional<Regnskapsfoerer> regnskapsfoerer = Optional.ofNullable(egenNæring.getRegnskapsfoerer());
        regnskapsfoerer.ifPresent(r -> egenNæringBuilder.medRegnskapsførerNavn(r.getNavn()).medRegnskapsførerTlf(r.getTelefon()));

        egenNæringBuilder.medBegrunnelse(egenNæring.getBeskrivelseAvEndring())
            .medEndringDato(egenNæring.getEndringsDato())
            .medNyoppstartet(egenNæring.isErNyoppstartet())
            .medNyIArbeidslivet(egenNæring.isErNyIArbeidslivet())
            .medVarigEndring(egenNæring.isErVarigEndring())
            .medNærRelasjon(egenNæring.isNaerRelasjon() != null && egenNæring.isNaerRelasjon());
        if (egenNæring.getNaeringsinntektBrutto() != null) {
            egenNæringBuilder.medBruttoInntekt(new BigDecimal(egenNæring.getNaeringsinntektBrutto()));
        }
        return egenNæringBuilder;
    }

    private DatoIntervallEntitet mapPeriode(Periode periode) {
        LocalDate fom = periode.getFom();
        LocalDate tom = periode.getTom();
        if (tom == null) {
            return DatoIntervallEntitet.fraOgMed(fom);
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    private void byggFødselsrelaterteFelter(Foedsel fødsel, FamilieHendelseBuilder hendelseBuilder) {
        if (fødsel.getFoedselsdato() == null) {
            throw new IllegalArgumentException("Utviklerfeil: Ved fødsel skal det være eksakt én fødselsdato");
        }

        LocalDate fødselsdato = fødsel.getFoedselsdato();
        if (fødsel.getTermindato() != null) {
            hendelseBuilder.medTerminbekreftelse(hendelseBuilder.getTerminbekreftelseBuilder()
                .medTermindato(fødsel.getTermindato()));
        }
        int antallBarn = fødsel.getAntallBarn();
        List<LocalDate> fødselsdatoene = new ArrayList<>();
        for (int i = 1; i <= antallBarn; i++) {
            fødselsdatoene.add(fødselsdato);
        }

        hendelseBuilder.medAntallBarn(antallBarn);
        for (LocalDate localDate : fødselsdatoene) {
            hendelseBuilder.leggTilBarn(localDate);
        }
    }

    private void byggTerminrelaterteFelter(Termin termin, FamilieHendelseBuilder hendelseBuilder) {
        Objects.requireNonNull(termin.getTermindato(), "Termindato må være oppgitt");

        hendelseBuilder.medAntallBarn(termin.getAntallBarn());
        hendelseBuilder.medTerminbekreftelse(hendelseBuilder.getTerminbekreftelseBuilder()
            .medTermindato(termin.getTermindato())
            .medUtstedtDato(termin.getUtstedtdato()));
    }

    private void byggOmsorgsovertakelsesrelaterteFelter(Omsorgsovertakelse omsorgsovertakelse, FamilieHendelseBuilder hendelseBuilder, SøknadEntitet.Builder søknadBuilder) {
        List<LocalDate> fødselsdatoene = omsorgsovertakelse.getFoedselsdato();

        hendelseBuilder.medAntallBarn(omsorgsovertakelse.getAntallBarn());
        final FamilieHendelseBuilder.AdopsjonBuilder familieHendelseAdopsjon = hendelseBuilder.getAdopsjonBuilder()
            .medOmsorgsovertakelseDato(omsorgsovertakelse.getOmsorgsovertakelsesdato());
        for (LocalDate localDate : fødselsdatoene) {
            hendelseBuilder.leggTilBarn(localDate);
        }
        hendelseBuilder.erOmsorgovertagelse();
        hendelseBuilder.medAdopsjon(familieHendelseAdopsjon);

        // Må også settes på søknad
        søknadBuilder.medFarSøkerType(tolkFarSøkerType(omsorgsovertakelse.getOmsorgsovertakelseaarsak()));
    }

    private FarSøkerType tolkFarSøkerType(Omsorgsovertakelseaarsaker omsorgsovertakelseaarsaker) {
        return FarSøkerType.fraKode(omsorgsovertakelseaarsaker.getKode());
    }


    private void byggAdopsjonsrelaterteFelter(Adopsjon adopsjon, FamilieHendelseBuilder hendelseBuilder) {
        List<LocalDate> fødselsdatoene = adopsjon.getFoedselsdato();

        hendelseBuilder.medAntallBarn(adopsjon.getAntallBarn());
        final FamilieHendelseBuilder.AdopsjonBuilder familieHendelseAdopsjon = hendelseBuilder.getAdopsjonBuilder()
            .medAnkomstDato(adopsjon.getAnkomstdato())
            .medErEktefellesBarn(adopsjon.isAdopsjonAvEktefellesBarn())
            .medOmsorgsovertakelseDato(adopsjon.getOmsorgsovertakelsesdato());
        for (LocalDate localDate : fødselsdatoene) {
            hendelseBuilder.leggTilBarn(localDate);
        }
        hendelseBuilder.medAdopsjon(familieHendelseAdopsjon);
    }

    private void byggMedlemskap(MottattDokumentWrapperSøknad skjema, Long behandlingId, LocalDate forsendelseMottatt) {
        Medlemskap medlemskap;
        Ytelse omYtelse = skjema.getOmYtelse();
        LocalDate mottattDato = skjema.getSkjema().getMottattDato();
        MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder()
            .medOppholdNå(true).medOppgittDato(forsendelseMottatt);

        if (omYtelse instanceof Engangsstønad) { // NOSONAR - ok måte å finne riktig JAXB-type
            medlemskap = ((Engangsstønad) omYtelse).getMedlemskap();
        } else if (omYtelse instanceof Foreldrepenger) { // NOSONAR - ok måte å finne riktig JAXB-type
            medlemskap = ((Foreldrepenger) omYtelse).getMedlemskap();
        } else if (omYtelse instanceof Svangerskapspenger) { // NOSONAR - ok måte å finne riktig JAXB-type
            medlemskap = ((Svangerskapspenger) omYtelse).getMedlemskap();
        } else {
            throw new IllegalStateException("Ytelsestype er ikke støttet");
        }
        Boolean iNorgeVedFoedselstidspunkt = medlemskap.isINorgeVedFoedselstidspunkt();
        oppgittTilknytningBuilder.medOppholdNå(Boolean.TRUE.equals(iNorgeVedFoedselstidspunkt));

        Objects.requireNonNull(medlemskap, "Medlemskap må være oppgitt");

        settOppholdUtlandPerioder(medlemskap, mottattDato, oppgittTilknytningBuilder);
        settOppholdNorgePerioder(medlemskap, mottattDato, oppgittTilknytningBuilder);
        medlemskapRepository.lagreOppgittTilkytning(behandlingId, oppgittTilknytningBuilder.build());
    }

    private void settOppholdUtlandPerioder(Medlemskap medlemskap, LocalDate mottattDato, MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder) {
        medlemskap.getOppholdUtlandet().forEach(opphUtl -> {
            boolean tidligereOpphold = opphUtl.getPeriode().getFom().isBefore(mottattDato);
            oppgittTilknytningBuilder.leggTilOpphold(byggUtlandsopphold(opphUtl, tidligereOpphold));
        });
    }

    private MedlemskapOppgittLandOppholdEntitet byggUtlandsopphold(OppholdUtlandet utenlandsopphold, boolean tidligereOpphold) {
        return new MedlemskapOppgittLandOppholdEntitet.Builder()
            .medLand(finnLandkode(utenlandsopphold.getLand().getKode()))
            .medPeriode(
                utenlandsopphold.getPeriode().getFom(),
                utenlandsopphold.getPeriode().getTom()
            )
            .erTidligereOpphold(tidligereOpphold)
            .build();
    }

    private void settOppholdNorgePerioder(Medlemskap medlemskap, LocalDate mottattDato, MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder) {
        medlemskap.getOppholdNorge().forEach(opphNorge -> {
            boolean tidligereOpphold = opphNorge.getPeriode().getFom().isBefore(mottattDato);
            MedlemskapOppgittLandOppholdEntitet oppholdNorgeSistePeriode = new MedlemskapOppgittLandOppholdEntitet.Builder()
                .erTidligereOpphold(tidligereOpphold)
                .medLand(Landkoder.NOR)
                .medPeriode(opphNorge.getPeriode().getFom(), opphNorge.getPeriode().getTom())
                .build();
            oppgittTilknytningBuilder.leggTilOpphold(oppholdNorgeSistePeriode);
        });
    }

    private SoekersRelasjonTilBarnet getSoekersRelasjonTilBarnet(MottattDokumentWrapperSøknad skjema) {
        SoekersRelasjonTilBarnet relasjonTilBarnet = null;
        Ytelse omYtelse = skjema.getOmYtelse();
        if (omYtelse instanceof Foreldrepenger) { // NOSONAR
            relasjonTilBarnet = ((Foreldrepenger) omYtelse).getRelasjonTilBarnet();
        } else if (omYtelse instanceof Engangsstønad) { // NOSONAR
            relasjonTilBarnet = ((Engangsstønad) omYtelse).getSoekersRelasjonTilBarnet();
        }

        Objects.requireNonNull(relasjonTilBarnet, "Relasjon til barnet må være oppgitt");
        return relasjonTilBarnet;
    }

    private Språkkode getSpraakValg(MottattDokumentWrapperSøknad skjema) {
        return Språkkode.defaultNorsk(skjema.getSpråkvalg() == null ? null : skjema.getSpråkvalg().getKode());
    }

    private SøknadEntitet.Builder byggFelleselementerForSøknad(SøknadEntitet.Builder søknadBuilder,
                                                               MottattDokumentWrapperSøknad skjemaWrapper,
                                                               Boolean elektroniskSøknad,
                                                               LocalDate forsendelseMottatt,
                                                               Optional<LocalDate> gjelderFra) {
        søknadBuilder.medElektroniskRegistrert(elektroniskSøknad)
            .medMottattDato(forsendelseMottatt)
            .medBegrunnelseForSenInnsending(skjemaWrapper.getBegrunnelseForSenSoeknad())
            .medTilleggsopplysninger(skjemaWrapper.getTilleggsopplysninger())
            .medSøknadsdato(gjelderFra.orElse(forsendelseMottatt))
            .medSpråkkode(getSpraakValg(skjemaWrapper));

        for (Vedlegg vedlegg : skjemaWrapper.getPåkrevdVedleggListe()) {
            byggSøknadVedlegg(søknadBuilder, vedlegg, true);
        }

        for (Vedlegg vedlegg : skjemaWrapper.getIkkePåkrevdVedleggListe()) {
            byggSøknadVedlegg(søknadBuilder, vedlegg, false);
        }

        return søknadBuilder;
    }

    private boolean skalByggeSøknadAnnenPart(MottattDokumentWrapperSøknad skjema) {
        AnnenForelder annenForelder = null;
        if (skjema.getOmYtelse() instanceof Foreldrepenger) {
            annenForelder = ((Foreldrepenger) skjema.getOmYtelse()).getAnnenForelder();
            return (annenForelder != null);
        } else if (skjema.getOmYtelse() instanceof Engangsstønad) {
            annenForelder = ((Engangsstønad) skjema.getOmYtelse()).getAnnenForelder();
        }
        return (annenForelder != null);
    }

    private void byggSøknadAnnenPart(MottattDokumentWrapperSøknad skjema, Behandling behandling) {
        OppgittAnnenPartBuilder oppgittAnnenPartBuilder = new OppgittAnnenPartBuilder();

        AnnenForelder annenForelder = null;

        if (skjema.getOmYtelse() instanceof Foreldrepenger) {
            annenForelder = ((Foreldrepenger) skjema.getOmYtelse()).getAnnenForelder();
        } else if (skjema.getOmYtelse() instanceof Engangsstønad) {
            annenForelder = ((Engangsstønad) skjema.getOmYtelse()).getAnnenForelder();
        }

        if (annenForelder instanceof AnnenForelderMedNorskIdent) { // NOSONAR - ok måte å finne riktig JAXB-type
            AnnenForelderMedNorskIdent annenForelderMedNorskIdent = (AnnenForelderMedNorskIdent) annenForelder;
            oppgittAnnenPartBuilder.medAktørId(new AktørId(annenForelderMedNorskIdent.getAktoerId()));

        } else if (annenForelder instanceof AnnenForelderUtenNorskIdent) { // NOSONAR - ok måte å finne riktig JAXB-type
            AnnenForelderUtenNorskIdent annenForelderUtenNorskIdent = (AnnenForelderUtenNorskIdent) annenForelder;
            String annenPartIdentString = annenForelderUtenNorskIdent.getUtenlandskPersonidentifikator();
            if (PersonIdent.erGyldigFnr(annenPartIdentString)) {
                tpsTjeneste.hentAktørForFnr(new PersonIdent(annenPartIdentString))
                    .filter(a -> !behandling.getAktørId().equals(a))
                    .ifPresent(oppgittAnnenPartBuilder::medAktørId);
            }
            oppgittAnnenPartBuilder.medUtenlandskFnr(annenPartIdentString);
            Optional<String> funnetLandkode = Optional.ofNullable(annenForelderUtenNorskIdent.getLand()).map(Land::getKode);
            funnetLandkode.ifPresent(s -> oppgittAnnenPartBuilder.medUtenlandskFnrLand(finnLandkode(s)));
        }

        personopplysningRepository.lagre(behandling.getId(), oppgittAnnenPartBuilder);

    }

    private Landkoder finnLandkode(String landKode) {
        return Landkoder.fraKode(landKode);
    }

    private void byggSøknadVedlegg(SøknadEntitet.Builder søknadBuilder, Vedlegg vedlegg, boolean påkrevd) {
        SøknadVedleggEntitet.Builder vedleggBuilder = new SøknadVedleggEntitet.Builder()
            .medErPåkrevdISøknadsdialog(påkrevd)
            .medInnsendingsvalg(tolkInnsendingsvalg(vedlegg.getInnsendingstype()))
            .medSkjemanummer(vedlegg.getSkjemanummer())
            .medTilleggsinfo(vedlegg.getTilleggsinformasjon());
        søknadBuilder.leggTilVedlegg(vedleggBuilder.build());
    }

    private Innsendingsvalg tolkInnsendingsvalg(Innsendingstype innsendingstype) {
        return Innsendingsvalg.fraKode(innsendingstype.getKode());
    }
}
