package no.nav.foreldrepenger.web.app.tjenester.registrering;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdateringTransisjon;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.mottak.registrerer.DokumentRegistrererTjeneste;
import no.nav.foreldrepenger.søknad.v3.SøknadConstants;
import no.nav.foreldrepenger.xmlutils.JaxbHelper;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.xml.soeknad.v3.ObjectFactory;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ManuellRegistreringDto.class, adapter = AksjonspunktOppdaterer.class)
public class ManuellRegistreringOppdaterer implements AksjonspunktOppdaterer<ManuellRegistreringDto> {

    private static final Logger LOG = LoggerFactory.getLogger(ManuellRegistreringOppdaterer.class);

    private FagsakRepository fagsakRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private DokumentRegistrererTjeneste dokumentRegistrererTjeneste;

    private Instance<SøknadMapper> søknadMappere;

    ManuellRegistreringOppdaterer() {
        // CDI
    }

    @Inject
    public ManuellRegistreringOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                         HistorikkinnslagRepository historikkinnslagRepository,
                                         DokumentRegistrererTjeneste dokumentRegistrererTjeneste,
                                         @Any Instance<SøknadMapper> søknadMappere) {
        this.søknadMappere = søknadMappere;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.dokumentRegistrererTjeneste = dokumentRegistrererTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(ManuellRegistreringDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingReferanse = param.getRef();
        var resultatBuilder = OppdateringResultat.utenTransisjon();

        if (dto.getUfullstendigSoeknad()) {
            if (behandlingReferanse.erRevurdering()) {
                // Vi ønsker ikke mangelfulle søknader i revurderinger ettersom det ikke gitt at de skal til Opphør/Avslag
                // Vurder å gå videre som en vanlig revurdering - dvs returner resultatBuilder.build() - men kan føre til automatisk vedtak uten brev.
                // Foreslår derfor henleggelse + manuell revurdering i feilmelding. Evt brev utenom løsningen.
                LOG.warn("Papirsøknad ufullstendig for revurdering i sak {} ytelse {}. Si fra på daglig overvåkning",
                    behandlingReferanse.saksnummer().getVerdi(), behandlingReferanse.fagsakYtelseType());
                throw new FunksjonellException("FP-093926", "Kan ikke registrere mangelfull søknad i revurdering.",
                    "Henlegg behandlingen og opprett eventuelt en revurdering fra meny.");
            }
            lagHistorikkInnslag(behandlingReferanse, "Søknad er mangelfull", null);
            return resultatBuilder
                .leggTilAvslåttVilkårRegistrering(VilkårType.SØKERSOPPLYSNINGSPLIKT, Avslagsårsak.MANGLENDE_DOKUMENTASJON)
                .medFremoverHopp(AksjonspunktOppdateringTransisjon.AVSLAG_VILKÅR)
                .build();
        }

        ManuellRegistreringValidator.validerOpplysninger(dto);
        if (FagsakYtelseType.FORELDREPENGER.equals(behandlingReferanse.fagsakYtelseType())) {
            ManuellRegistreringValidator.validerAktivitetskrav(dto, behandlingReferanse.relasjonRolle());
        }

        var fagsak = fagsakRepository.finnEksaktFagsak(behandlingReferanse.fagsakId());
        var navBruker = fagsak.getNavBruker();
        var søknadXml = opprettSøknadsskjema(dto, behandlingReferanse, navBruker);
        var dokumentTypeId = finnDokumentType(dto, behandlingReferanse.behandlingType());

        dokumentRegistrererTjeneste.aksjonspunktManuellRegistrering(behandlingReferanse, søknadXml, dokumentTypeId, dto.getMottattDato());
        if (dto.isRegistrerVerge()) {
            resultatBuilder.medEkstraAksjonspunktResultat(AksjonspunktDefinisjon.AVKLAR_VERGE, AksjonspunktStatus.OPPRETTET);
        }

        lagHistorikkInnslag(behandlingReferanse, "Registrer papirsøknad", dto.getKommentarEndring());
        return resultatBuilder.build();
    }

    private DokumentTypeId finnDokumentType(ManuellRegistreringDto dto, BehandlingType behandlingType) {
        var søknadsType = dto.getSoknadstype().getKode();

        if (FagsakYtelseType.ENGANGSTØNAD.getKode().equals(søknadsType)) {
            if (erFødsel(dto)) {
                return DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;
            }
            if (erAdopsjon(dto)) {
                return DokumentTypeId.SØKNAD_ENGANGSSTØNAD_ADOPSJON;
            }
        } else if (FagsakYtelseType.FORELDREPENGER.getKode().equals(søknadsType)) {
            if (erEndringssøknad(behandlingType)) {
                return DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD;
            }
            if (erFødsel(dto)) {
                return DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
            }
            if (erAdopsjon(dto)) {
                return DokumentTypeId.SØKNAD_FORELDREPENGER_ADOPSJON;
            }
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.getKode().equals(søknadsType)) {
            return DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER;

        }
        return DokumentTypeId.UDEFINERT;
    }

    private boolean erEndringssøknad(BehandlingType behandlingType) {
        return behandlingType.equals(BehandlingType.REVURDERING);
    }

    private boolean erAdopsjon(ManuellRegistreringDto dto) {
        return FamilieHendelseType.ADOPSJON.getKode().equals(dto.getTema().getKode()) || FamilieHendelseType.OMSORG.getKode()
            .equals(dto.getTema().getKode());
    }

    private boolean erFødsel(ManuellRegistreringDto dto) {
        return FamilieHendelseType.FØDSEL.getKode().equals(dto.getTema().getKode());
    }

    private String opprettSøknadsskjema(ManuellRegistreringDto dto, BehandlingReferanse behandlingReferanse, NavBruker navBruker) {

        var ytelseType = behandlingReferanse.fagsakYtelseType();
        var behandlingType = behandlingReferanse.behandlingType();

        if (REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER.equals(dto.getAksjonspunktDefinisjon())) {
            // minihack for
            behandlingType = BehandlingType.REVURDERING;
        }

        var mapper = finnSøknadMapper(ytelseType, behandlingType);
        var søknad = mapper.mapSøknad(dto, navBruker);

        try {
            return JaxbHelper.marshalAndValidateJaxb(SøknadConstants.JAXB_CLASS,
                new ObjectFactory().createSoeknad(søknad),
                SøknadConstants.XSD_LOCATION,
                SøknadConstants.ADDITIONAL_XSD_LOCATION,
                SøknadConstants.ADDITIONAL_CLASSES);
        } catch (JAXBException | SAXException e) {
            throw new TekniskException("FP-453254", "Feil ved marshalling av søknadsskjema", e);
        }
    }

    private void lagHistorikkInnslag(BehandlingReferanse ref, String tittel, String kommentarEndring) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(tittel)
            .addLinje(kommentarEndring)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    public SøknadMapper finnSøknadMapper(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(SøknadMapper.class, søknadMappere, ytelseType, behandlingType).orElseThrow();
    }
}
