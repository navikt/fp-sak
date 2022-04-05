package no.nav.foreldrepenger.behandling.revurdering.ytelse.es;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingFeil;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
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
    public RevurderingTjenesteImpl(BehandlingRepository behandlingRepository,
                                   BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider,
                                   BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                   @FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD) RevurderingEndring revurderingEndring,
                                   RevurderingTjenesteFelles revurderingTjenesteFelles,
                                   VergeRepository vergeRepository) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.familieHendelseRepository = grunnlagRepositoryProvider.getFamilieHendelseRepository();
        this.personopplysningRepository = grunnlagRepositoryProvider.getPersonopplysningRepository();
        this.medlemskapRepository = grunnlagRepositoryProvider.getMedlemskapRepository();
        this.søknadRepository = grunnlagRepositoryProvider.getSøknadRepository();
        this.opptjeningIUtlandDokStatusRepository = grunnlagRepositoryProvider.getOpptjeningIUtlandDokStatusRepository();
        this.revurderingEndring = revurderingEndring;
        this.revurderingTjenesteFelles = revurderingTjenesteFelles;
        this.vergeRepository = vergeRepository;
    }

    @Override
    public Behandling opprettManuellRevurdering(Fagsak fagsak,
                                                BehandlingÅrsakType revurderingsÅrsak,
                                                OrganisasjonsEnhet enhet) {
        return opprettRevurdering(fagsak, revurderingsÅrsak, true, enhet);
    }

    @Override
    public Behandling opprettAutomatiskRevurdering(Fagsak fagsak,
                                                   BehandlingÅrsakType revurderingsÅrsak,
                                                   OrganisasjonsEnhet enhet) {
        return opprettRevurdering(fagsak, revurderingsÅrsak, false, enhet);
    }

    private Behandling opprettRevurdering(Fagsak fagsak,
                                          BehandlingÅrsakType revurderingsÅrsak,
                                          boolean manueltOpprettet,
                                          OrganisasjonsEnhet enhet) {
        var opprinneligBehandlingOptional = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(
            fagsak.getId());
        if (opprinneligBehandlingOptional.isEmpty()) {
            throw RevurderingFeil.tjenesteFinnerIkkeBehandlingForRevurdering(fagsak.getId());
        }
        var opprinneligBehandling = opprinneligBehandlingOptional.get();
        // lås original behandling først slik at ingen andre forsøker på samme
        behandlingskontrollTjeneste.initBehandlingskontroll(opprinneligBehandling);

        // deretter opprett kontekst for revurdering og opprett
        var revurderingBehandling = revurderingTjenesteFelles.opprettRevurderingsbehandling(revurderingsÅrsak,
            opprinneligBehandling, manueltOpprettet, enhet);
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(revurderingBehandling);
        behandlingskontrollTjeneste.opprettBehandling(kontekst, revurderingBehandling);

        // Kopier søknadsdata
        søknadRepository.hentSøknadHvisEksisterer(opprinneligBehandling.getId())
            .ifPresent(s -> søknadRepository.lagreOgFlush(revurderingBehandling, s));
        kopierAlleGrunnlagFraTidligereBehandling(opprinneligBehandling, revurderingBehandling);
        return revurderingBehandling;
    }

    @Override
    public void kopierAlleGrunnlagFraTidligereBehandling(Behandling original, Behandling ny) {
        var orginalBehandlingId = original.getId();
        var nyBehandlingId = ny.getId();
        if (BehandlingType.REVURDERING.equals(ny.getType())) {
            familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(orginalBehandlingId,
                nyBehandlingId);
            if (ny.harBehandlingÅrsak(
                BehandlingÅrsakType.RE_HENDELSE_FØDSEL)) { // Unngå manuell re-evaluering i tilfelle "automatisk" revurdering
                personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
                medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
                vergeRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
            } else {
                personopplysningRepository.kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(orginalBehandlingId,
                    nyBehandlingId);
                medlemskapRepository.kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(orginalBehandlingId,
                    nyBehandlingId);
            }
        } else {
            familieHendelseRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
            personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
            medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
            vergeRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId, nyBehandlingId);
        }
        opptjeningIUtlandDokStatusRepository.kopierGrunnlagFraEksisterendeBehandling(orginalBehandlingId,
            nyBehandlingId);
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
