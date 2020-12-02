package no.nav.foreldrepenger.behandling.revurdering.ytelse.es;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingFeil;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.utlanddok.OpptjeningIUtlandDokStatusRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

@FagsakYtelseTypeRef("ES")
@ApplicationScoped
public class RevurderingTjenesteImpl implements RevurderingTjeneste {
    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private FamilieHendelseRepository familieHendelseRepository;
    private PersonopplysningRepository personopplysningRepository;
    private MedlemskapRepository medlemskapRepository;
    private SøknadRepository søknadRepository;
    private OpptjeningIUtlandDokStatusRepository opptjeningIUtlandDokStatusRepository;
    private RevurderingTjenesteFelles revurderingTjenesteFelles;
    private RevurderingEndring revurderingEndring;
    private VergeRepository vergeRepository;

    public RevurderingTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    public RevurderingTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
            BehandlingskontrollTjeneste behandlingskontrollTjeneste,
            @FagsakYtelseTypeRef("ES") RevurderingEndring revurderingEndring,
            RevurderingTjenesteFelles revurderingTjenesteFelles,
            VergeRepository vergeRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.opptjeningIUtlandDokStatusRepository = repositoryProvider.getOpptjeningIUtlandDokStatusRepository();
        this.revurderingEndring = revurderingEndring;
        this.revurderingTjenesteFelles = revurderingTjenesteFelles;
        this.vergeRepository = vergeRepository;
    }

    @Override
    public Behandling opprettManuellRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet) {
        return opprettRevurdering(fagsak, revurderingsÅrsak, true, enhet);
    }

    @Override
    public Behandling opprettAutomatiskRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet) {
        return opprettRevurdering(fagsak, revurderingsÅrsak, false, enhet);
    }

    private Behandling opprettRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingÅrsakType, boolean manueltOpprettet,
            OrganisasjonsEnhet enhet) {
        Optional<Behandling> opprinneligBehandlingOptional = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId());
        if (!opprinneligBehandlingOptional.isPresent()) {
            throw RevurderingFeil.FACTORY.tjenesteFinnerIkkeBehandlingForRevurdering(fagsak.getId()).toException();
        }
        Behandling opprinneligBehandling = opprinneligBehandlingOptional.get();
        // lås original behandling først slik at ingen andre forsøker på samme
        behandlingskontrollTjeneste.initBehandlingskontroll(opprinneligBehandling);

        // deretter opprett kontekst for revurdering og opprett
        Behandling revurderingBehandling = revurderingTjenesteFelles.opprettRevurderingsbehandling(revurderingÅrsakType, opprinneligBehandling,
                manueltOpprettet, enhet);
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(revurderingBehandling);
        behandlingskontrollTjeneste.opprettBehandling(kontekst, revurderingBehandling);

        // Kopier søknadsdata
        final Optional<SøknadEntitet> søknadOptional = søknadRepository.hentSøknadHvisEksisterer(opprinneligBehandling.getId());
        søknadOptional.ifPresent(s -> søknadRepository.lagreOgFlush(revurderingBehandling, s));
        kopierAlleGrunnlagFraTidligereBehandling(opprinneligBehandling, revurderingBehandling);
        return revurderingBehandling;
    }

    @Override
    public void kopierAlleGrunnlagFraTidligereBehandling(Behandling original, Behandling ny) {
        Long orginalBehandlingId = original.getId();
        Long nyBehandlingId = ny.getId();
        if (BehandlingType.REVURDERING.equals(ny.getType())) {
            familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandlingForRevurdering(orginalBehandlingId, nyBehandlingId);
            if (ny.harBehandlingÅrsak(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)) { // Unngå manuell re-evaluering i tilfelle "automatisk" revurdering
                personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
                medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
                vergeRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
            } else {
                personopplysningRepository.kopierGrunnlagFraEksisterendeBehandlingForRevurdering(orginalBehandlingId, nyBehandlingId);
                medlemskapRepository.kopierGrunnlagFraEksisterendeBehandlingForRevurdering(orginalBehandlingId, nyBehandlingId);
            }
        } else {
            familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
            personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
            medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
            vergeRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
        }
        opptjeningIUtlandDokStatusRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
    }

    @Override
    public Boolean kanRevurderingOpprettes(Fagsak fagsak) {
        return revurderingTjenesteFelles.kanRevurderingOpprettes(fagsak);
    }

    @Override
    public boolean erRevurderingMedUendretUtfall(Behandling behandling) {
        return revurderingEndring.erRevurderingMedUendretUtfall(behandling);
    }

}
