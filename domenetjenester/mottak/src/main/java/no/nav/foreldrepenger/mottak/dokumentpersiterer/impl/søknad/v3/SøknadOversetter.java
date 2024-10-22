package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3;

import static java.util.Objects.nonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpAvklartOpphold;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdÅrsak;

import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Tilrettelegging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.Innsendingsvalg;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadVedleggEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingFomKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilans;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittUtenlandskVirksomhet;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.MottattDokumentOversetter;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.NamespaceRef;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.SøknadDataFraTidligereVedtakTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;
import no.nav.foreldrepenger.søknad.v3.SøknadConstants;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.xml.soeknad.endringssoeknad.v3.Endringssoeknad;
import no.nav.vedtak.felles.xml.soeknad.engangsstoenad.v3.Engangsstønad;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Adopsjon;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Bruker;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Foedsel;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Medlemskap;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Omsorgsovertakelse;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.OppholdUtlandet;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Periode;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.SoekersRelasjonTilBarnet;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Termin;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Vedlegg;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.AnnenOpptjening;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.EgenNaering;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Foreldrepenger;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Frilans;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.NorskOrganisasjon;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Opptjening;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.UtenlandskArbeidsforhold;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.UtenlandskOrganisasjon;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Innsendingstype;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.MorsAktivitetsTyper;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Omsorgsovertakelseaarsaker;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Virksomhetstyper;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Arbeidsforhold;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Frilanser;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.SelvstendigNæringsdrivende;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Svangerskapspenger;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Gradering;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.LukketPeriodeMedVedlegg;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Oppholdsperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Overfoeringsperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Person;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Utsettelsesperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Uttaksperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Virksomhet;

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
            byggYtelsesSpesifikkeFelterForEndringssøknad(omYtelse, behandling, mottattDato);
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
        var loggFør = loggFamilieHendelseForFørstegangPåRevurdering(behandling, "FØR");
        var hendelseBuilder = familieHendelseRepository.opprettBuilderFor(behandling.getId());
        var søknadBuilder = new SøknadEntitet.Builder();
        byggFelleselementerForSøknad(søknadBuilder, wrapper, elektroniskSøknad, mottattDato, Optional.empty());
        var behandlingId = behandling.getId();
        var aktørId = behandling.getAktørId();
        if (wrapper.getOmYtelse() != null) {
            byggMedlemskap(wrapper, behandlingId, mottattDato);
        }
        lagreAnnenPart(wrapper, behandling);
        byggYtelsesSpesifikkeFelter(wrapper, behandling);
        byggOpptjeningsspesifikkeFelter(wrapper, behandling);
        if (wrapper.getOmYtelse() instanceof Svangerskapspenger svangerskapspenger) {
            byggFamilieHendelseForSvangerskap(svangerskapspenger, hendelseBuilder);
        } else {
            var soekersRelasjonTilBarnet = getSoekersRelasjonTilBarnet(wrapper);
            if (soekersRelasjonTilBarnet instanceof Foedsel foedsel) {
                byggFødselsrelaterteFelter(foedsel, hendelseBuilder);
            } else if (soekersRelasjonTilBarnet instanceof Termin termin) {
                byggTerminrelaterteFelter(termin, hendelseBuilder);
            } else if (soekersRelasjonTilBarnet instanceof Adopsjon adopsjon) {
                byggAdopsjonsrelaterteFelter(adopsjon, hendelseBuilder);
            } else if (soekersRelasjonTilBarnet instanceof Omsorgsovertakelse omsorgsovertakelse) {
                byggOmsorgsovertakelsesrelaterteFelter(omsorgsovertakelse, hendelseBuilder, søknadBuilder);
            }
        }
        familieHendelseRepository.lagre(behandling.getId(), hendelseBuilder);
        søknadBuilder.medErEndringssøknad(false);
        var relasjonsRolleType = utledRolle(behandling.getFagsakYtelseType(), wrapper.getBruker(), behandlingId, aktørId);
        var søknad = søknadBuilder.medRelasjonsRolleType(relasjonsRolleType).build();
        søknadRepository.lagreOgFlush(behandling, søknad);
        fagsakRepository.oppdaterRelasjonsRolle(behandling.getFagsakId(), søknad.getRelasjonsRolleType());
        if (loggFør) {
            loggFamilieHendelseForFørstegangPåRevurdering(behandling, "ETTER");
        }
    }

    private boolean loggFamilieHendelseForFørstegangPåRevurdering(Behandling behandling, String infix) {
        var aggregat = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId());
        if (aggregat.isEmpty()) {
            return false;
        }
        var harBekreftetFamiliehendelse = aggregat.flatMap(FamilieHendelseGrunnlagEntitet::getGjeldendeBekreftetVersjon).isPresent();
        var søknadFamilieHendelseType = aggregat.map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon).map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT);
        var gjeldendeFamilieHendelseType = aggregat.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon).map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT);
        LocalDate søknadFamiliehendelseDato = null;
        LocalDate gjeldendeFamiliehendelseDato = null;
        try {
            søknadFamiliehendelseDato = aggregat.map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon)
                .map(FamilieHendelseEntitet::getSkjæringstidspunkt).orElse(null);
            gjeldendeFamiliehendelseDato = aggregat.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                .map(FamilieHendelseEntitet::getSkjæringstidspunkt).orElse(null);
        } catch (Exception e) {
            // Intentionally empty
        }
        LOG.info("OversettFørstegang {} {}: type {} dato {} gjeldende type {} dato {} for bId {} type {} yt {}", infix,
            harBekreftetFamiliehendelse ? "bekreftet" : "søknad", søknadFamilieHendelseType.getKode(), søknadFamiliehendelseDato,
            gjeldendeFamilieHendelseType.getKode(), gjeldendeFamiliehendelseDato, behandling.getId(), behandling.getType().getKode(),
            behandling.getFagsakYtelseType().getKode());
        return true;
    }

    private void byggFamilieHendelseForSvangerskap(Svangerskapspenger omYtelse,
                                                   FamilieHendelseBuilder hendelseBuilder) {
        var termindato = omYtelse.getTermindato();
        Objects.requireNonNull(termindato, "Termindato må være oppgitt");
        hendelseBuilder.medTerminbekreftelse(hendelseBuilder.getTerminbekreftelseBuilder().medTermindato(termindato));
        var fødselsdato = omYtelse.getFødselsdato();
        if (fødselsdato != null) {
            hendelseBuilder.erFødsel().medFødselsDato(fødselsdato).medAntallBarn(1);
        }
    }

    private RelasjonsRolleType utledRolle(FagsakYtelseType ytelseType, Bruker bruker, Long behandlingId, AktørId aktørId) {
        var kjønn = personinfoAdapter.hentBrukerKjønnForAktør(ytelseType, aktørId)
            .map(PersoninfoKjønn::getKjønn)
            .orElseThrow(() -> {
                var msg = String.format("Søknad på behandling %s mangler RelasjonsRolleType", behandlingId);
                throw new TekniskException("FP-931148", msg);
            });

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

    private void byggYtelsesSpesifikkeFelterForEndringssøknad(Endringssoeknad omYtelse,
                                                              Behandling behandling,
                                                              LocalDate mottattDato) {
        var fordeling = omYtelse.getFordeling();
        var perioder = fordeling.getPerioder();
        var annenForelderErInformert = fordeling.isAnnenForelderErInformert();
        var ønskerJustertVedFødsel = fordeling.isOenskerJustertVedFoedsel();
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medOppgittFordeling(lagOppgittFordeling(behandling, perioder, annenForelderErInformert, mottattDato, ønskerJustertVedFødsel));
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());
    }

    private void byggYtelsesSpesifikkeFelter(SøknadWrapper skjemaWrapper,
                                             Behandling behandling) {
        var søknadMottattDato = skjemaWrapper.getSkjema().getMottattDato();
        if (skjemaWrapper.getOmYtelse() instanceof Foreldrepenger omYtelse) {
            var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
                .medOppgittFordeling(oversettFordeling(behandling, omYtelse, søknadMottattDato));
            if (!behandling.erRevurdering()) {
                yfBuilder.medOppgittDekningsgrad(oversettDekningsgrad(omYtelse))
                    .medSakskompleksDekningsgrad(null);
            }
            oversettRettighet(omYtelse).ifPresent(yfBuilder::medOppgittRettighet);
            ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());
        } else if (skjemaWrapper.getOmYtelse() instanceof Svangerskapspenger svangerskapspenger) {
            oversettOgLagreTilretteleggingOgVurderEksisterende(svangerskapspenger, behandling, søknadMottattDato);
        }
    }

    private void oversettOgLagreTilretteleggingOgVurderEksisterende(Svangerskapspenger svangerskapspenger,
                                                                    Behandling behandling, LocalDate søknadMottattDato) {
        var brukMottattTidspunkt = Optional.ofNullable(søknadMottattDato)
            .filter(d -> !d.equals(behandling.getOpprettetTidspunkt().toLocalDate()))
            .map(LocalDate::atStartOfDay)
            .orElseGet(behandling::getOpprettetTidspunkt);
        var svpBuilder = new SvpGrunnlagEntitet.Builder().medBehandlingId(behandling.getId());
        List<SvpTilretteleggingEntitet> nyeTilrettelegginger = new ArrayList<>();

        var tilretteleggingListe = svangerskapspenger.getTilretteleggingListe().getTilrettelegging();

        for (var tilrettelegging : tilretteleggingListe) {
            var builder = new SvpTilretteleggingEntitet.Builder();
            builder.medBehovForTilretteleggingFom(tilrettelegging.getBehovForTilretteleggingFom())
                .medKopiertFraTidligereBehandling(false)
                .medMottattTidspunkt(brukMottattTidspunkt)
                .medAvklarteOpphold(mapAvtaltFerie(tilrettelegging, svangerskapspenger));

            tilrettelegging.getHelTilrettelegging()
                .forEach(helTilrettelegging -> builder.medHelTilrettelegging(helTilrettelegging.getTilrettelagtArbeidFom(),
                    brukMottattTidspunkt.toLocalDate(), SvpTilretteleggingFomKilde.SØKNAD));

            tilrettelegging.getDelvisTilrettelegging()
                .forEach(delvisTilrettelegging -> builder.medDelvisTilrettelegging(delvisTilrettelegging.getTilrettelagtArbeidFom(),
                    delvisTilrettelegging.getStillingsprosent(), brukMottattTidspunkt.toLocalDate(), SvpTilretteleggingFomKilde.SØKNAD));

            tilrettelegging.getIngenTilrettelegging()
                .forEach(ingenTilrettelegging -> builder.medIngenTilrettelegging(ingenTilrettelegging.getSlutteArbeidFom(),
                    brukMottattTidspunkt.toLocalDate(), SvpTilretteleggingFomKilde.SØKNAD));

            oversettArbeidsforhold(builder, tilrettelegging.getArbeidsforhold());
            nyeTilrettelegginger.add(builder.build());
        }

        //I mangel på endringssøknad forsøker vi å flette eventuelle eksisterende tilrettelegginger med nye hvis mulig
        var eksisterendeTilrettelegginger = svangerskapspengerRepository.hentGrunnlag(behandling.getId())
            .map(SvpGrunnlagEntitet::getGjeldendeVersjon)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe)
            .orElse(Collections.emptyList());
        List<SvpTilretteleggingEntitet> nyeOgEksisterendeTilrettelegginger = new ArrayList<>();

        if (!eksisterendeTilrettelegginger.isEmpty()) {
            var inputEksisterendeTilretteleggingMap = eksisterendeTilrettelegginger.stream().collect(Collectors.groupingBy(this::tilretteleggingNøkkel)); // Kortlevet
            var inputNyeTilretteleggingMap = nyeTilrettelegginger.stream().collect(Collectors.groupingBy(this::tilretteleggingNøkkel)); // Kortlevet

            var heltNyeMap = nyeTilrettelegginger.stream()
                .filter(nt -> inputEksisterendeTilretteleggingMap.get(tilretteleggingNøkkel(nt)) == null)
                .collect(Collectors.groupingBy(this::tilretteleggingNøkkel));

            heltNyeMap.forEach((key, value) -> nyeOgEksisterendeTilrettelegginger.addAll(value));

            var bareGamleMap = eksisterendeTilrettelegginger.stream()
                .filter(gt -> inputNyeTilretteleggingMap.get(tilretteleggingNøkkel(gt)) == null)
                .collect(Collectors.groupingBy(this::tilretteleggingNøkkel));

            bareGamleMap.forEach((key, value) -> value.forEach(tlr -> {
                var eksisterendeTilR = new SvpTilretteleggingEntitet.Builder(tlr).medKopiertFraTidligereBehandling(true).build();
                nyeOgEksisterendeTilrettelegginger.add(eksisterendeTilR);
            }));

            var fletteNyeMap = nyeTilrettelegginger.stream()
                .filter(nt -> inputEksisterendeTilretteleggingMap.get(tilretteleggingNøkkel(nt)) != null)
                .collect(Collectors.toMap(this::tilretteleggingNøkkel, Function.identity()));

            var fletteGamleMap = eksisterendeTilrettelegginger.stream()
                .filter(nt -> inputNyeTilretteleggingMap.get(tilretteleggingNøkkel(nt)) != null)
                .collect(Collectors.groupingBy(this::tilretteleggingNøkkel));

            for (var eksTilrettelegging : fletteGamleMap.entrySet()) {
                var nyTilrettelegging = fletteNyeMap.get(eksTilrettelegging.getKey());
                eksTilrettelegging.getValue().forEach(eksTlr -> nyeOgEksisterendeTilrettelegginger.add(
                    oppdaterEksisterendeTlrMedNyeFomsOgOpphold(nyTilrettelegging, eksTlr)));
            }
        } else {
            //ingen eksisterende
            nyeOgEksisterendeTilrettelegginger.addAll(nyeTilrettelegginger);
        }

        var svpGrunnlag = svpBuilder.medOpprinneligeTilrettelegginger(nyeOgEksisterendeTilrettelegginger).build();
        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
    }

    private String tilretteleggingNøkkel(SvpTilretteleggingEntitet tilrettelegging) {
        return tilrettelegging.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElseGet(() -> tilrettelegging.getArbeidType().getKode());
    }

    private SvpTilretteleggingEntitet oppdaterEksisterendeTlrMedNyeFomsOgOpphold(SvpTilretteleggingEntitet nyTlR,
                                                                                 SvpTilretteleggingEntitet eksisterendeTlr) {
        var nyFomListe = new ArrayList<>(nyTlR.getTilretteleggingFOMListe());
        var tidligsteNyFom = nyFomListe.stream().map(TilretteleggingFOM::getFomDato).min(LocalDate::compareTo).orElse(LocalDate.EPOCH);
        var eksisterendeFOMSomSkalKopieres =  eksisterendeTlr.getTilretteleggingFOMListe().stream().filter(f -> f.getFomDato().isBefore(tidligsteNyFom)).toList();

        eksisterendeFOMSomSkalKopieres.forEach(eksFom ->
            nyFomListe.add(new TilretteleggingFOM.Builder()
                .fraEksisterende(eksFom)
                .medTidligstMottattDato(Optional.ofNullable(eksFom.getTidligstMotattDato())
                    .orElseGet(() -> eksisterendeTlr.getMottattTidspunkt().toLocalDate()))
                    .medKilde(SvpTilretteleggingFomKilde.TIDLIGERE_VEDTAK)
                .build()));

        nyFomListe.sort(Comparator.comparing(TilretteleggingFOM::getFomDato));

        var justerteOppholdsperioder = eksisterendeTlr.getAvklarteOpphold().stream()
            .filter(opphold -> opphold.getFom().isBefore(tidligsteNyFom))
            .map(opphold -> {
                if (!opphold.getTom().isBefore(tidligsteNyFom)) {
                    return SvpAvklartOpphold.Builder.nytt()
                        .medKilde(opphold.getKilde())
                        .medOppholdÅrsak(opphold.getOppholdÅrsak())
                        .medOppholdPeriode(opphold.getFom(), tidligsteNyFom.minusDays(1))
                        .build();
                }
                return opphold;
            }).collect(Collectors.toList());
        var justertTilrettelegging = SvpTilretteleggingEntitet.Builder.fraEksisterende(eksisterendeTlr)
            .medBehovForTilretteleggingFom(nyFomListe.stream().map(TilretteleggingFOM::getFomDato).min(LocalDate::compareTo).orElse(null))
            .medTilretteleggingFraDatoer(nyFomListe)
            .medAvklarteOpphold(justerteOppholdsperioder);
        nyTlR.getAvklarteOpphold().forEach(justertTilrettelegging::medAvklartOpphold);
        return justertTilrettelegging.build();
    }

    private void oversettArbeidsforhold(SvpTilretteleggingEntitet.Builder builder, Arbeidsforhold arbeidsforhold) {
        if (arbeidsforhold instanceof no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Arbeidsgiver arbeidsgiverType) {
            builder.medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
            Arbeidsgiver arbeidsgiver;
            if (arbeidsforhold instanceof no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Virksomhet virksomhetType) {
                var orgnr = virksomhetType.getIdentifikator();
                virksomhetTjeneste.hentOrganisasjon(orgnr);
                arbeidsgiver = Arbeidsgiver.virksomhet(orgnr);
            } else {
                var arbeidsgiverIdent = new PersonIdent(arbeidsgiverType.getIdentifikator());
                var aktørIdArbeidsgiver = personinfoAdapter.hentAktørForFnr(arbeidsgiverIdent);
                if (aktørIdArbeidsgiver.isEmpty()) {
                    throw new TekniskException("FP-545381",
                        "Fant ikke personident for arbeidsgiver som er privatperson i PDL");
                }
                arbeidsgiver = Arbeidsgiver.person(aktørIdArbeidsgiver.get());
            }
            builder.medArbeidsgiver(arbeidsgiver);
        } else if (arbeidsforhold instanceof Frilanser frilanser) {
            builder.medArbeidType(ArbeidType.FRILANSER);
            builder.medOpplysningerOmRisikofaktorer(frilanser.getOpplysningerOmRisikofaktorer());
            builder.medOpplysningerOmTilretteleggingstiltak(
                ((Frilanser) arbeidsforhold).getOpplysningerOmTilretteleggingstiltak());
        } else if (arbeidsforhold instanceof SelvstendigNæringsdrivende selvstendig) {
            builder.medArbeidType(ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE);
            builder.medOpplysningerOmTilretteleggingstiltak(selvstendig.getOpplysningerOmTilretteleggingstiltak());
            builder.medOpplysningerOmRisikofaktorer(selvstendig.getOpplysningerOmRisikofaktorer());
        } else {
            throw new TekniskException("FP-187531", "Ukjent type arbeidsforhold i svangerskapspengesøknad");
        }
    }

    private static List<SvpAvklartOpphold> mapAvtaltFerie(Tilrettelegging tilrettelegging,
                                                          Svangerskapspenger svp) {
        if (tilrettelegging.getArbeidsforhold() instanceof no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Arbeidsgiver arbeidsgiver) {
            var identifikator = arbeidsgiver.getIdentifikator();
            var avtalteFerier = svp.getAvtaltFerieListe().getAvtaltFerie();
            return avtalteFerier.stream()
                .filter(af -> Objects.equals(identifikator, af.getArbeidsgiver().getIdentifikator()))
                .map(ao -> SvpAvklartOpphold.Builder.nytt()
                    .medKilde(SvpOppholdKilde.SØKNAD)
                    .medOppholdÅrsak(SvpOppholdÅrsak.FERIE)
                    .medOppholdPeriode(ao.getAvtaltFerieFom(), ao.getAvtaltFerieTom())
                    .build())
                .toList();
        } else {
            return List.of();
        }
    }

    private void byggOpptjeningsspesifikkeFelter(SøknadWrapper skjemaWrapper, Behandling behandling) {
        var behandlingId = behandling.getId();
        Opptjening opptjeningFraSøknad = null;
        if (skjemaWrapper.getOmYtelse() instanceof final Foreldrepenger omYtelse) {
            opptjeningFraSøknad = omYtelse.getOpptjening();
        } else if (skjemaWrapper.getOmYtelse() instanceof final Svangerskapspenger omYtelse) {
            opptjeningFraSøknad = omYtelse.getOpptjening();
        }

        if (opptjeningFraSøknad != null && (!opptjeningFraSøknad.getUtenlandskArbeidsforhold().isEmpty()
            || !opptjeningFraSøknad.getAnnenOpptjening().isEmpty() || !opptjeningFraSøknad.getEgenNaering().isEmpty() || nonNull(
            opptjeningFraSøknad.getFrilans()))) {
            Optional<InntektArbeidYtelseGrunnlag> iayGrunnlag = BehandlingType.REVURDERING.equals(behandling.getType()) ? iayTjeneste.finnGrunnlag(behandlingId) : Optional.empty();
            var eksisterendeOppgittOpptjening = iayGrunnlag.flatMap(InntektArbeidYtelseGrunnlag::getGjeldendeOppgittOpptjening);
            var erOverstyrt = iayGrunnlag.flatMap(InntektArbeidYtelseGrunnlag::getOverstyrtOppgittOpptjening).isPresent();
            if (eksisterendeOppgittOpptjening.isPresent()) {
                LOG.info("Fletter eksisterende oppgitt opptjening med ny data fra søknad for behandling med id {} ytelse {}", behandlingId, behandling.getFagsakYtelseType().getKode());
                var flettetOppgittOpptjening = flettOppgittOpptjening(opptjeningFraSøknad, eksisterendeOppgittOpptjening.get());
                var erEndringAvOppgittOpptjening = IAYGrunnlagDiff.erEndringPåOppgittOpptjening(eksisterendeOppgittOpptjening, Optional.of(flettetOppgittOpptjening.build()));
                if (!erEndringAvOppgittOpptjening) {
                    return;
                }
                if (erOverstyrt) {
                    iayTjeneste.lagreOppgittOpptjeningNullstillOverstyring(behandlingId, flettetOppgittOpptjening);
                } else {
                    iayTjeneste.lagreOppgittOpptjening(behandlingId, flettetOppgittOpptjening);
                }
            } else {
                var nyOppgittOpptjening = mapOppgittOpptjening(opptjeningFraSøknad);
                iayTjeneste.lagreOppgittOpptjening(behandlingId, nyOppgittOpptjening);
            }

        }
    }

    private Optional<OppgittRettighetEntitet> oversettRettighet(Foreldrepenger omYtelse) {
        return Optional.ofNullable(omYtelse.getRettigheter())
            .map(rettigheter -> new OppgittRettighetEntitet(rettigheter.isHarAnnenForelderRett(), rettigheter.isHarAleneomsorgForBarnet(),
                    harOppgittUføreEllerPerioderMedAktivitetUføre(omYtelse, rettigheter.isHarMorUforetrygd()), rettigheter.isHarAnnenForelderTilsvarendeRettEOS(),
                rettigheter.isHarAnnenForelderOppholdtSegIEOS()));
    }

    // TODO: Avklare med AP om dette er rett måte å serve rettighet??? Info må uansett sjekke oppgitt fordeling for eldre tilfelle (med mindre vi kjører DB-oppdatering)
    private static boolean harOppgittUføreEllerPerioderMedAktivitetUføre(Foreldrepenger omYtelse, Boolean oppgittUføre) {
        return oppgittUføre != null && oppgittUføre || omYtelse.getFordeling()
            .getPerioder()
            .stream()
            .anyMatch(p -> p instanceof Uttaksperiode uttak && erPeriodeMedAktivitetUføre(uttak.getMorsAktivitetIPerioden())
                || p instanceof Utsettelsesperiode utsettelse && erPeriodeMedAktivitetUføre(utsettelse.getMorsAktivitetIPerioden()));
    }

    private static boolean erPeriodeMedAktivitetUføre(MorsAktivitetsTyper morsAktivitet) {
        return morsAktivitet != null && MorsAktivitet.UFØRE.getKode().equals(morsAktivitet.getKode());
    }

    private OppgittFordelingEntitet oversettFordeling(Behandling behandling,
                                                      Foreldrepenger omYtelse,
                                                      LocalDate mottattDato) {
        var oppgittePerioder = new ArrayList<>(omYtelse.getFordeling().getPerioder());
        var annenForelderErInformert = omYtelse.getFordeling().isAnnenForelderErInformert();
        var ønskerJustertVedFødsel = omYtelse.getFordeling().isOenskerJustertVedFoedsel();
        return lagOppgittFordeling(behandling, oppgittePerioder, annenForelderErInformert, mottattDato, ønskerJustertVedFødsel);
    }

    private OppgittFordelingEntitet lagOppgittFordeling(Behandling behandling,
                                                        List<LukketPeriodeMedVedlegg> perioder,
                                                        boolean annenForelderErInformert,
                                                        LocalDate mottattDatoFraSøknad,
                                                        Boolean ønskerJustertVedFødsel) {

        var oppgittPerioder = perioder.stream()
            .map(this::oversettPeriode)
            .filter(this::inneholderVirkedager)
            .toList();
        var filtrertPerioder = søknadDataFraTidligereVedtakTjeneste.filtrerVekkPerioderSomErLikeInnvilgetUttak(behandling, oppgittPerioder);
        var perioderMedTidligstMottatt = søknadDataFraTidligereVedtakTjeneste.oppdaterTidligstMottattDato(behandling, mottattDatoFraSøknad, filtrertPerioder);
        var perioderMedGodkjentVurdering = søknadDataFraTidligereVedtakTjeneste.oppdaterMedGodkjenteDokumentasjonsVurderinger(behandling, perioderMedTidligstMottatt);
        if (!inneholderVirkedager(perioderMedGodkjentVurdering)) {
            throw new IllegalArgumentException("Fordelingen må inneholde perioder med minst en virkedag");
        }
        return new OppgittFordelingEntitet(perioderMedGodkjentVurdering, annenForelderErInformert, Objects.equals(ønskerJustertVedFødsel, true));
    }

    private boolean inneholderVirkedager(List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream().anyMatch(this::inneholderVirkedager);
    }

    private boolean inneholderVirkedager(OppgittPeriodeEntitet periode) {
        return Virkedager.beregnAntallVirkedager(periode.getFom(), periode.getTom()) > 0;
    }

    private Dekningsgrad oversettDekningsgrad(Foreldrepenger omYtelse) {
        var dekingsgrad = omYtelse.getDekningsgrad().getDekningsgrad();
        if (Integer.toString(Dekningsgrad._80.getVerdi()).equalsIgnoreCase(dekingsgrad.getKode())) {
            return Dekningsgrad._80;
        }
        if (Integer.toString(Dekningsgrad._100.getVerdi()).equalsIgnoreCase(dekingsgrad.getKode())) {
            return Dekningsgrad._100;
        }
        throw new IllegalArgumentException("Ukjent dekningsgrad " + dekingsgrad.getKode());
    }

    private OppgittPeriodeEntitet oversettPeriode(LukketPeriodeMedVedlegg lukketPeriode) {
        var oppgittPeriodeBuilder = OppgittPeriodeBuilder.ny().medPeriode(lukketPeriode.getFom(), lukketPeriode.getTom());
        if (lukketPeriode instanceof final Uttaksperiode periode) {
            oversettUttakperiode(oppgittPeriodeBuilder, periode);
        } else if (lukketPeriode instanceof Oppholdsperiode oppholdsperiode) {
            oppgittPeriodeBuilder.medÅrsak(OppholdÅrsak.fraKode(oppholdsperiode.getAarsak().getKode()));
            oppgittPeriodeBuilder.medPeriodeType(UttakPeriodeType.UDEFINERT);
        } else if (lukketPeriode instanceof Overfoeringsperiode overfoeringsperiode) {
            oppgittPeriodeBuilder.medÅrsak(OverføringÅrsak.fraKode(overfoeringsperiode.getAarsak().getKode()));
            oppgittPeriodeBuilder.medPeriodeType(UttakPeriodeType.fraKode(overfoeringsperiode.getOverfoeringAv().getKode()));
        } else if (lukketPeriode instanceof Utsettelsesperiode utsettelsesperiode) {
            oversettUtsettelsesperiode(oppgittPeriodeBuilder, utsettelsesperiode);
        } else {
            throw new IllegalStateException("Ukjent periodetype.");
        }
        return oppgittPeriodeBuilder.build();
    }

    private void oversettUtsettelsesperiode(OppgittPeriodeBuilder oppgittPeriodeBuilder,
                                            Utsettelsesperiode utsettelsesperiode) {
        if (utsettelsesperiode.getUtsettelseAv() != null) {
            oppgittPeriodeBuilder.medPeriodeType(
                UttakPeriodeType.fraKode(utsettelsesperiode.getUtsettelseAv().getKode()));
        }
        oppgittPeriodeBuilder.medÅrsak(UtsettelseÅrsak.fraKode(utsettelsesperiode.getAarsak().getKode()));
        if (utsettelsesperiode.getMorsAktivitetIPerioden() != null) {
            oppgittPeriodeBuilder.medMorsAktivitet(
                MorsAktivitet.fraKode(utsettelsesperiode.getMorsAktivitetIPerioden().getKode()));
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
            oppgittPeriodeBuilder.medSamtidigUttaksprosent(
                new SamtidigUttaksprosent(periode.getSamtidigUttakProsent()));
        } else if (periode instanceof Gradering gradering) {
            oversettGradering(oppgittPeriodeBuilder, gradering);
        }
        if (periode.getMorsAktivitetIPerioden() != null && !periode.getMorsAktivitetIPerioden().getKode().isEmpty()) {
            oppgittPeriodeBuilder.medMorsAktivitet(
                MorsAktivitet.fraKode(periode.getMorsAktivitetIPerioden().getKode()));
        }
    }

    private boolean erSamtidigUttak(Uttaksperiode periode) {
        return periode.isOenskerSamtidigUttak() != null && periode.isOenskerSamtidigUttak();
    }

    private void oversettGradering(OppgittPeriodeBuilder oppgittPeriodeBuilder, Gradering gradering) {
        var arbeidsgiverFraSøknad = gradering.getArbeidsgiver();
        if (arbeidsgiverFraSøknad != null) {
            var arbeidsgiver = oversettArbeidsgiver(arbeidsgiverFraSøknad);
            oppgittPeriodeBuilder.medArbeidsgiver(arbeidsgiver);
        }

        if (!gradering.isErArbeidstaker() && !gradering.isErFrilanser() && !gradering.isErSelvstNæringsdrivende()) {
            throw new IllegalArgumentException("Graderingsperioder må enten ha valgt at/fl/sn");
        }

        oppgittPeriodeBuilder.medGraderingAktivitetType(GraderingAktivitetType.from(gradering.isErArbeidstaker(), gradering.isErFrilanser(),
            gradering.isErSelvstNæringsdrivende()));
        oppgittPeriodeBuilder.medArbeidsprosent(BigDecimal.valueOf(gradering.getArbeidtidProsent()));
    }

    private Arbeidsgiver oversettArbeidsgiver(no.nav.vedtak.felles.xml.soeknad.uttak.v3.Arbeidsgiver arbeidsgiverFraSøknad) {
        if (arbeidsgiverFraSøknad instanceof Person) {
            var aktørId = personinfoAdapter.hentAktørForFnr(PersonIdent.fra(arbeidsgiverFraSøknad.getIdentifikator()));
            if (aktørId.isEmpty()) {
                throw new IllegalStateException("Finner ikke arbeidsgiver");
            }
            return Arbeidsgiver.person(aktørId.get());
        }
        if (arbeidsgiverFraSøknad instanceof Virksomhet) {
            var orgnr = arbeidsgiverFraSøknad.getIdentifikator();
            virksomhetTjeneste.hentOrganisasjon(orgnr);
            return Arbeidsgiver.virksomhet(orgnr);
        }
        throw new IllegalStateException("Ukjent arbeidsgiver type " + arbeidsgiverFraSøknad.getClass());
    }

    private OppgittOpptjeningBuilder flettOppgittOpptjening(Opptjening opptjening, OppgittOpptjening eksisterendeOppgittOpptjening) {
        // Bygger ny opptjening fra gammelt grunnlag for å ta vare på gamle opplysninger, så lenge equals metoder er rett vil ikke dette gi dobble innslag
        var flettetBuilder = OppgittOpptjeningBuilder.oppdater(Optional.of(eksisterendeOppgittOpptjening));
        var opptjeningFraNySøknad = mapOppgittOpptjening(opptjening).build();

        // Erstatter eksiterende frilans om finnes
        opptjeningFraNySøknad.getFrilans().ifPresent(flettetBuilder::leggTilFrilansOpplysninger);

        // Legger til nye perioder med annen aktivitet (type eller periode må være ulik)
        opptjeningFraNySøknad.getAnnenAktivitet().stream()
            .filter(aa -> !eksisterendeOppgittOpptjening.getAnnenAktivitet().contains(aa))
            .forEach(flettetBuilder::leggTilAnnenAktivitet);

        // Legger til nye næringer, eller erstatter næring med samme orgnr
        opptjeningFraNySøknad.getEgenNæring().forEach(flettetBuilder::leggTilEllerErstattEgenNæring);

        // Legger til nye perioder med oppgitt arbeidsforhold (type, utenlandskVirksomhet eller periode må være ulik)
        opptjeningFraNySøknad.getOppgittArbeidsforhold()
            .stream().filter(oa -> !eksisterendeOppgittOpptjening.getOppgittArbeidsforhold().contains(oa))
            .forEach(flettetBuilder::leggTilOppgittArbeidsforhold);

        return flettetBuilder;
    }

    private OppgittOpptjeningBuilder mapOppgittOpptjening(Opptjening opptjening) {
        var builder = OppgittOpptjeningBuilder.ny();
        opptjening.getAnnenOpptjening()
            .forEach(annenOpptjening -> builder.leggTilAnnenAktivitet(mapAnnenAktivitet(annenOpptjening)));
        opptjening.getEgenNaering().forEach(egenNaering -> builder.leggTilEgenNæring(mapEgenNæring(egenNaering)));
        opptjening.getUtenlandskArbeidsforhold()
            .forEach(arbeidsforhold -> builder.leggTilOppgittArbeidsforhold(
                mapOppgittUtenlandskArbeidsforhold(arbeidsforhold)));
        if (nonNull(opptjening.getFrilans())) {
            opptjening.getFrilans()
                .getPeriode()
                .forEach(periode -> builder.leggTilAnnenAktivitet(mapFrilansPeriode(periode)));
            builder.leggTilFrilansOpplysninger(mapFrilansOpplysninger(opptjening.getFrilans()));
        }
        return builder;
    }

    private OppgittFrilans mapFrilansOpplysninger(Frilans frilans) {
        var builder = OppgittOpptjeningBuilder.OppgittFrilansBuilder.ny()
            .medErNyoppstartet(frilans.isErNyoppstartet())
            .medHarInntektFraFosterhjem(frilans.isHarInntektFraFosterhjem())
            .medHarNærRelasjon(frilans.isNaerRelasjon());
        frilans.getFrilansoppdrag().stream()
            .map(fo -> new OppgittFrilansoppdrag(fo.getOppdragsgiver(), mapPeriode(fo.getPeriode())))
            .forEach(builder::leggTilFrilansoppdrag);
        return builder.build();
    }

    private OppgittArbeidsforhold mapOppgittUtenlandskArbeidsforhold(
        UtenlandskArbeidsforhold utenlandskArbeidsforhold) {
        var builder = OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny();
        var landkode = finnLandkode(utenlandskArbeidsforhold.getArbeidsland().getKode());
        builder.medUtenlandskVirksomhet(
            new OppgittUtenlandskVirksomhet(landkode, utenlandskArbeidsforhold.getArbeidsgiversnavn()));
        builder.medErUtenlandskInntekt(true);
        builder.medArbeidType(ArbeidType.UTENLANDSK_ARBEIDSFORHOLD);

        var periode = mapPeriode(utenlandskArbeidsforhold.getPeriode());
        builder.medPeriode(periode);
        return builder.build();
    }

    private OppgittAnnenAktivitet mapFrilansPeriode(Periode periode) {
        var datoIntervallEntitet = mapPeriode(periode);
        return new OppgittAnnenAktivitet(datoIntervallEntitet, ArbeidType.FRILANSER);
    }

    private OppgittAnnenAktivitet mapAnnenAktivitet(AnnenOpptjening annenOpptjening) {
        var datoIntervallEntitet = mapPeriode(annenOpptjening.getPeriode());
        var type = annenOpptjening.getType();

        var arbeidType = ArbeidType.fraKode(type.getKode());
        return new OppgittAnnenAktivitet(datoIntervallEntitet, arbeidType);
    }

    private List<OppgittOpptjeningBuilder.EgenNæringBuilder> mapEgenNæring(EgenNaering egenNæring) {
        List<OppgittOpptjeningBuilder.EgenNæringBuilder> builders = new ArrayList<>();
        egenNæring.getVirksomhetstype()
            .forEach(virksomhettype -> builders.add(mapEgenNæringForType(egenNæring, virksomhettype)));
        return builders;
    }

    private OppgittOpptjeningBuilder.EgenNæringBuilder mapEgenNæringForType(EgenNaering egenNæring,
                                                                            Virksomhetstyper virksomhettype) {
        var egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny();
        if (egenNæring instanceof NorskOrganisasjon norskOrganisasjon) {
            var orgNr = norskOrganisasjon.getOrganisasjonsnummer();
            virksomhetTjeneste.hentOrganisasjon(orgNr);
            egenNæringBuilder.medVirksomhet(orgNr);
        } else {
            var utenlandskOrganisasjon = (UtenlandskOrganisasjon) egenNæring;
            var landkode = finnLandkode(utenlandskOrganisasjon.getRegistrertILand().getKode());
            egenNæringBuilder.medUtenlandskVirksomhet(
                new OppgittUtenlandskVirksomhet(landkode, utenlandskOrganisasjon.getNavn()));
        }

        // felles
        var virksomhetType = VirksomhetType.fraKode(virksomhettype.getKode());
        egenNæringBuilder.medPeriode(mapPeriode(egenNæring.getPeriode())).medVirksomhetType(virksomhetType);

        var regnskapsfoerer = Optional.ofNullable(egenNæring.getRegnskapsfoerer());
        regnskapsfoerer.ifPresent(
            r -> egenNæringBuilder.medRegnskapsførerNavn(r.getNavn()).medRegnskapsførerTlf(r.getTelefon()));

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
        var fom = periode.getFom();
        var tom = periode.getTom();
        if (tom == null) {
            return DatoIntervallEntitet.fraOgMed(fom);
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    private void byggFødselsrelaterteFelter(Foedsel fødsel, FamilieHendelseBuilder hendelseBuilder) {
        if (fødsel.getFoedselsdato() == null) {
            throw new IllegalArgumentException("Utviklerfeil: Ved fødsel skal det være eksakt én fødselsdato");
        }

        var fødselsdato = fødsel.getFoedselsdato();
        if (fødsel.getTermindato() != null) {
            hendelseBuilder.medTerminbekreftelse(
                hendelseBuilder.getTerminbekreftelseBuilder().medTermindato(fødsel.getTermindato()));
        }
        var antallBarn = fødsel.getAntallBarn();
        hendelseBuilder.tilbakestillBarn().medAntallBarn(antallBarn);
        for (var i = 1; i <= antallBarn; i++) {
            hendelseBuilder.leggTilBarn(fødselsdato);
        }
    }

    private void byggTerminrelaterteFelter(Termin termin, FamilieHendelseBuilder hendelseBuilder) {
        Objects.requireNonNull(termin.getTermindato(), "Termindato må være oppgitt");

        hendelseBuilder.medAntallBarn(termin.getAntallBarn());
        hendelseBuilder.medTerminbekreftelse(hendelseBuilder.getTerminbekreftelseBuilder()
            .medTermindato(termin.getTermindato())
            .medUtstedtDato(termin.getUtstedtdato()));
    }

    private void byggOmsorgsovertakelsesrelaterteFelter(Omsorgsovertakelse omsorgsovertakelse,
                                                        FamilieHendelseBuilder hendelseBuilder,
                                                        SøknadEntitet.Builder søknadBuilder) {
        var fødselsdatoene = omsorgsovertakelse.getFoedselsdato();

        hendelseBuilder.tilbakestillBarn().medAntallBarn(omsorgsovertakelse.getAntallBarn());
        var familieHendelseAdopsjon = hendelseBuilder.getAdopsjonBuilder().medOmsorgsovertakelseDato(omsorgsovertakelse.getOmsorgsovertakelsesdato());
        for (var localDate : fødselsdatoene) {
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
        var fødselsdatoene = adopsjon.getFoedselsdato();

        hendelseBuilder.tilbakestillBarn().medAntallBarn(adopsjon.getAntallBarn());
        var familieHendelseAdopsjon = hendelseBuilder.getAdopsjonBuilder()
            .medAnkomstDato(adopsjon.getAnkomstdato())
            .medErEktefellesBarn(adopsjon.isAdopsjonAvEktefellesBarn())
            .medOmsorgsovertakelseDato(adopsjon.getOmsorgsovertakelsesdato());
        for (var localDate : fødselsdatoene) {
            hendelseBuilder.leggTilBarn(localDate);
        }
        hendelseBuilder.medAdopsjon(familieHendelseAdopsjon);
    }

    private void byggMedlemskap(SøknadWrapper skjema, Long behandlingId, LocalDate forsendelseMottatt) {
        Medlemskap medlemskap;
        var omYtelse = skjema.getOmYtelse();
        var mottattDato = skjema.getSkjema().getMottattDato();
        var oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder().medOppholdNå(true)
            .medOppgittDato(forsendelseMottatt);

        if (omYtelse instanceof Engangsstønad engangsstønad) {
            medlemskap = engangsstønad.getMedlemskap();
        } else if (omYtelse instanceof Foreldrepenger foreldrepenger) {
            medlemskap = foreldrepenger.getMedlemskap();
        } else if (omYtelse instanceof Svangerskapspenger svangerskapspenger) {
            medlemskap = svangerskapspenger.getMedlemskap();
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

    private void settOppholdUtlandPerioder(Medlemskap medlemskap,
                                           LocalDate mottattDato,
                                           MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder) {
        medlemskap.getOppholdUtlandet().forEach(opphUtl -> {
            var tidligereOpphold = opphUtl.getPeriode().getFom().isBefore(mottattDato);
            oppgittTilknytningBuilder.leggTilOpphold(byggUtlandsopphold(opphUtl, tidligereOpphold));
        });
    }

    private MedlemskapOppgittLandOppholdEntitet byggUtlandsopphold(OppholdUtlandet utenlandsopphold,
                                                                   boolean tidligereOpphold) {
        return new MedlemskapOppgittLandOppholdEntitet.Builder().medLand(
            finnLandkode(utenlandsopphold.getLand().getKode()))
            .medPeriode(utenlandsopphold.getPeriode().getFom(), utenlandsopphold.getPeriode().getTom())
            .erTidligereOpphold(tidligereOpphold)
            .build();
    }

    private void settOppholdNorgePerioder(Medlemskap medlemskap,
                                          LocalDate mottattDato,
                                          MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder) {
        medlemskap.getOppholdNorge().forEach(opphNorge -> {
            var tidligereOpphold = opphNorge.getPeriode().getFom().isBefore(mottattDato);
            var oppholdNorgeSistePeriode = new MedlemskapOppgittLandOppholdEntitet.Builder().erTidligereOpphold(
                tidligereOpphold)
                .medLand(Landkoder.NOR)
                .medPeriode(opphNorge.getPeriode().getFom(), opphNorge.getPeriode().getTom())
                .build();
            oppgittTilknytningBuilder.leggTilOpphold(oppholdNorgeSistePeriode);
        });
    }

    private SoekersRelasjonTilBarnet getSoekersRelasjonTilBarnet(SøknadWrapper skjema) {
        SoekersRelasjonTilBarnet relasjonTilBarnet = null;
        var omYtelse = skjema.getOmYtelse();
        if (omYtelse instanceof Foreldrepenger foreldrepenger) {
            relasjonTilBarnet = foreldrepenger.getRelasjonTilBarnet();
        } else if (omYtelse instanceof Engangsstønad engangsstønad) {
            relasjonTilBarnet = engangsstønad.getSoekersRelasjonTilBarnet();
        }

        Objects.requireNonNull(relasjonTilBarnet, "Relasjon til barnet må være oppgitt");
        return relasjonTilBarnet;
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
