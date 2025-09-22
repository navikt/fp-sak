package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.Innsendingsvalg;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadVedleggEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.MottattDokumentOversetter;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.NamespaceRef;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.SøknadDataFraTidligereVedtakTjeneste;
import no.nav.foreldrepenger.søknad.v3.SøknadConstants;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.xml.soeknad.endringssoeknad.v3.Endringssoeknad;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Bruker;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Vedlegg;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Foreldrepenger;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Innsendingstype;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Svangerskapspenger;

@NamespaceRef(SøknadConstants.NAMESPACE)
@ApplicationScoped
public class SøknadOversetter implements MottattDokumentOversetter<SøknadWrapper> {

    private static final Logger LOG = LoggerFactory.getLogger(SøknadOversetter.class);

    private VirksomhetTjeneste virksomhetTjeneste;
    private PersonopplysningRepository personopplysningRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private SøknadRepository søknadRepository;
    private MedlemskapRepository medlemskapRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private PersoninfoAdapter personinfoAdapter;
    private BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private FagsakRepository fagsakRepository;
    private SøknadDataFraTidligereVedtakTjeneste søknadDataFraTidligereVedtakTjeneste;
    private AnnenPartOversetter annenPartOversetter;

    @Inject
    public SøknadOversetter(FagsakRepository fagsakRepository,
                            BehandlingRevurderingTjeneste behandlingRevurderingTjeneste,
                            BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider,
                            VirksomhetTjeneste virksomhetTjeneste,
                            InntektArbeidYtelseTjeneste iayTjeneste,
                            PersoninfoAdapter personinfoAdapter,
                            SøknadDataFraTidligereVedtakTjeneste søknadDataFraTidligereVedtakTjeneste,
                            AnnenPartOversetter annenPartOversetter) {
        this.iayTjeneste = iayTjeneste;
        this.familieHendelseRepository = grunnlagRepositoryProvider.getFamilieHendelseRepository();
        this.søknadRepository = grunnlagRepositoryProvider.getSøknadRepository();
        this.medlemskapRepository = grunnlagRepositoryProvider.getMedlemskapRepository();
        this.personopplysningRepository = grunnlagRepositoryProvider.getPersonopplysningRepository();
        this.ytelsesFordelingRepository = grunnlagRepositoryProvider.getYtelsesFordelingRepository();
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.behandlingRevurderingTjeneste = behandlingRevurderingTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.svangerskapspengerRepository = grunnlagRepositoryProvider.getSvangerskapspengerRepository();
        this.søknadDataFraTidligereVedtakTjeneste = søknadDataFraTidligereVedtakTjeneste;
        this.annenPartOversetter = annenPartOversetter;
    }

    SøknadOversetter() {
        // for CDI proxy
    }

    @Override
    public void trekkUtDataOgPersister(SøknadWrapper wrapper,
                                       MottattDokument mottattDokument,
                                       Behandling behandling,
                                       Optional<LocalDate> gjelderFra) {
        if (wrapper.getOmYtelse() instanceof Endringssoeknad && !erEndring(mottattDokument)) {
            throw new IllegalArgumentException("Kan ikke sende inn en Endringssøknad uten å angi "
                + DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD.getKode() + " samtidig. Fikk "
                + mottattDokument.getDokumentType());
        }

        if (erEndring(mottattDokument)) {
            persisterEndringssøknad(wrapper, mottattDokument, behandling, gjelderFra);
        } else {
            persisterSøknad(wrapper, mottattDokument, behandling);
        }
    }

    private SøknadEntitet.Builder kopierSøknad(Behandling behandling) {
        SøknadEntitet.Builder søknadBuilder;
        var originalBehandlingIdOpt = behandling.getOriginalBehandlingId();
        if (originalBehandlingIdOpt.isPresent()) {
            var behandlingId = behandling.getId();
            var originalBehandlingId = originalBehandlingIdOpt.get();
            var originalSøknad = søknadRepository.hentSøknad(originalBehandlingId);
            søknadBuilder = new SøknadEntitet.Builder(originalSøknad, false);

            personopplysningRepository.hentOppgittAnnenPartHvisEksisterer(originalBehandlingId).ifPresent(oap -> {
                var oppgittAnnenPartBuilder = new OppgittAnnenPartBuilder(oap);
                personopplysningRepository.lagre(behandlingId, oppgittAnnenPartBuilder.build());
            });

            var oppgittTilknytning = medlemskapRepository.hentMedlemskap(behandlingId)
                .flatMap(MedlemskapAggregat::getOppgittTilknytning)
                .orElseThrow();
            var oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder(oppgittTilknytning);
            medlemskapRepository.lagreOppgittTilkytning(behandlingId, oppgittTilknytningBuilder.build());
        } else {
            søknadBuilder = new SøknadEntitet.Builder();
        }

        return søknadBuilder;
    }

    private void persisterEndringssøknad(SøknadWrapper wrapper,
                                         MottattDokument mottattDokument,
                                         Behandling behandling,
                                         Optional<LocalDate> gjelderFra) {
        var mottattDato = mottattDokument.getMottattDato();
        var elektroniskSøknad = mottattDokument.getElektroniskRegistrert();

        //Kopier og oppdater søknadsfelter.
        var søknadBuilder = kopierSøknad(behandling);
        byggFelleselementerForSøknad(søknadBuilder, wrapper, elektroniskSøknad, mottattDato, gjelderFra);
        var henlagteBehandlingerEtterInnvilget = behandlingRevurderingTjeneste.finnHenlagteBehandlingerEtterSisteInnvilgedeIkkeHenlagteBehandling(
            behandling.getFagsakId());
        if (!henlagteBehandlingerEtterInnvilget.isEmpty()) {
            søknadBuilder.medSøknadsdato(
                søknadRepository.hentSøknad(henlagteBehandlingerEtterInnvilget.get(0).getId()).getSøknadsdato());
        }

        if (wrapper.getOmYtelse() instanceof final Endringssoeknad omYtelse) {
            new ForeldrepengerUttakOversetter(ytelsesFordelingRepository, virksomhetTjeneste, personinfoAdapter, søknadDataFraTidligereVedtakTjeneste)
                .oversettForeldrepengerEndringssøknad(omYtelse, behandling, mottattDato);
        }
        søknadBuilder.medErEndringssøknad(true);
        var søknad = søknadBuilder.build();

        søknadRepository.lagreOgFlush(behandling, søknad);
    }

    private void persisterSøknad(SøknadWrapper wrapper,
                                 MottattDokument mottattDokument,
                                 Behandling behandling) {
        var mottattDato = mottattDokument.getMottattDato();
        var elektroniskSøknad = mottattDokument.getElektroniskRegistrert();
        var søknadBuilder = new SøknadEntitet.Builder();
        byggFelleselementerForSøknad(søknadBuilder, wrapper, elektroniskSøknad, mottattDato, Optional.empty());
        var behandlingId = behandling.getId();
        var aktørId = behandling.getAktørId();
        if (wrapper.getOmYtelse() != null) {
            new MedlemskapOversetter(medlemskapRepository).byggMedlemskap(wrapper, behandlingId, mottattDato);
        }
        lagreAnnenPart(wrapper, behandling);
        byggYtelsesSpesifikkeFelter(wrapper, behandling);
        new OpptjeningOversetter(virksomhetTjeneste, iayTjeneste).byggOpptjeningsspesifikkeFelter(wrapper, behandling);
        new FamilieHendelseOversetter(familieHendelseRepository).oversettPersisterFamilieHendelse(wrapper, behandling, søknadBuilder);
        søknadBuilder.medErEndringssøknad(false);
        var relasjonsRolleType = utledRolle(behandling.getFagsakYtelseType(), wrapper.getBruker(), behandling.getSaksnummer(), aktørId);
        var søknad = søknadBuilder.medRelasjonsRolleType(relasjonsRolleType).build();
        søknadRepository.lagreOgFlush(behandling, søknad);
        fagsakRepository.oppdaterRelasjonsRolle(behandling.getFagsakId(), søknad.getRelasjonsRolleType());
    }

    private RelasjonsRolleType utledRolle(FagsakYtelseType ytelseType, Bruker bruker, Saksnummer saksnummer, AktørId aktørId) {
        var kjønn = personinfoAdapter.hentBrukerKjønnForAktør(ytelseType, aktørId)
            .map(PersoninfoKjønn::getKjønn)
            .orElseThrow(() -> {
                var msg = String.format("Søker i sak %s mangler kjønn", saksnummer.getVerdi());
                return new TekniskException("FP-931148", msg);
            });

        // Korriger kjønnsbasert inntil videre. Klart mest vanlig med misforståelser. Vurder UDEFINERT og aksjonspunkt.
        // Mangler rolle i søknad, bruker kjønnsbasert antagelse
        if (bruker == null || bruker.getSoeknadsrolle() == null) {
            var valgtrolle = NavBrukerKjønn.MANN.equals(kjønn) ? RelasjonsRolleType.FARA : RelasjonsRolleType.MORA;
            LOG.info("Brukerrolle sak {} mangler søknadsrolle utledet {}", saksnummer.getVerdi(), valgtrolle);
            return valgtrolle;
        }
        // Samsvar rolle / kjønn
        if (ForeldreType.MOR.getKode().equals(bruker.getSoeknadsrolle().getKode()) && erKvinne(kjønn)) {
            return RelasjonsRolleType.MORA;
        }
        if (ForeldreType.FAR.getKode().equals(bruker.getSoeknadsrolle().getKode()) && erMann(kjønn)) {
            return RelasjonsRolleType.FARA;
        }
        if (ForeldreType.MEDMOR.getKode().equals(bruker.getSoeknadsrolle().getKode()) && erKvinne(kjønn)) {
            return RelasjonsRolleType.MEDMOR;
        }
        // Korriger rolle.
        var valgtrolle = NavBrukerKjønn.MANN.equals(kjønn) ? RelasjonsRolleType.FARA : RelasjonsRolleType.MORA;
        LOG.info("Brukerrolle sak {} oppgitt {} utledet {}", saksnummer.getVerdi(), bruker.getSoeknadsrolle().getKode(), valgtrolle);
        return valgtrolle;
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

    private void byggYtelsesSpesifikkeFelter(SøknadWrapper skjemaWrapper,
                                             Behandling behandling) {
        var søknadMottattDato = skjemaWrapper.getSkjema().getMottattDato();
        if (skjemaWrapper.getOmYtelse() instanceof Foreldrepenger omYtelse) {
            new ForeldrepengerUttakOversetter(ytelsesFordelingRepository, virksomhetTjeneste, personinfoAdapter, søknadDataFraTidligereVedtakTjeneste)
                .oversettForeldrepengerSøknad(omYtelse, behandling, søknadMottattDato);
        } else if (skjemaWrapper.getOmYtelse() instanceof Svangerskapspenger svangerskapspenger) {
            new TilretteleggingOversetter(svangerskapspengerRepository, virksomhetTjeneste, personinfoAdapter)
                .oversettOgLagreTilretteleggingOgVurderEksisterende(svangerskapspenger, behandling, søknadMottattDato);
        }
    }

    private Språkkode getSpraakValg(SøknadWrapper skjema) {
        return Språkkode.defaultNorsk(skjema.getSpråkvalg() == null ? null : skjema.getSpråkvalg().getKode());
    }

    private void byggFelleselementerForSøknad(SøknadEntitet.Builder søknadBuilder,
                                              SøknadWrapper skjemaWrapper,
                                              Boolean elektroniskSøknad,
                                              LocalDate forsendelseMottatt,
                                              Optional<LocalDate> gjelderFra) {
        søknadBuilder.medElektroniskRegistrert(elektroniskSøknad)
            .medMottattDato(forsendelseMottatt)
            .medBegrunnelseForSenInnsending(skjemaWrapper.getBegrunnelseForSenSoeknad())
            .medTilleggsopplysninger(skjemaWrapper.getTilleggsopplysninger())
            .medSøknadsdato(gjelderFra.orElse(forsendelseMottatt))
            .medSpråkkode(getSpraakValg(skjemaWrapper));

        for (var vedlegg : skjemaWrapper.getPåkrevdVedleggListe()) {
            byggSøknadVedlegg(søknadBuilder, vedlegg, true);
        }

        for (var vedlegg : skjemaWrapper.getIkkePåkrevdVedleggListe()) {
            byggSøknadVedlegg(søknadBuilder, vedlegg, false);
        }

    }

    private void lagreAnnenPart(SøknadWrapper skjema, Behandling behandling) {
        var oppgittAnnenPart = annenPartOversetter.oversett(skjema, behandling.getAktørId());
        oppgittAnnenPart.ifPresent(ap -> personopplysningRepository.lagre(behandling.getId(), ap));
    }

    static Landkoder finnLandkode(String landKode) {
        return Landkoder.fraKode(landKode);
    }

    private void byggSøknadVedlegg(SøknadEntitet.Builder søknadBuilder, Vedlegg vedlegg, boolean påkrevd) {
        var vedleggBuilder = new SøknadVedleggEntitet.Builder()
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
