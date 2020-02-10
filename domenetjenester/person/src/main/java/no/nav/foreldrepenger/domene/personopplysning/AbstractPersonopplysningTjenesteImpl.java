package no.nav.foreldrepenger.domene.personopplysning;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingsgrunnlagKodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public abstract class AbstractPersonopplysningTjenesteImpl implements StandardPersonopplysningTjeneste {

    private PersonopplysningRepository personopplysningRepository;
    private BehandlingsgrunnlagKodeverkRepository behandlingsgrunnlagKodeverkRepository;

    AbstractPersonopplysningTjenesteImpl() {
        // CDI
    }

    public AbstractPersonopplysningTjenesteImpl(PersonopplysningRepository personopplysningRepository, BehandlingsgrunnlagKodeverkRepository behandlingsgrunnlagKodeverkRepository) {
        this.personopplysningRepository = personopplysningRepository;
        this.behandlingsgrunnlagKodeverkRepository = behandlingsgrunnlagKodeverkRepository;
    }

    @Override
    public PersonopplysningerAggregat hentPersonopplysninger(BehandlingReferanse ref) {
        final LocalDate localDate = ref.getUtledetSkjæringstidspunkt();
        return hentGjeldendePersoninformasjonPåTidspunkt(ref.getBehandlingId(), ref.getAktørId(), localDate);
    }

    @Override
    public Optional<PersonopplysningerAggregat> hentPersonopplysningerHvisEksisterer(BehandlingReferanse ref) {
        final Optional<PersonopplysningGrunnlagEntitet> grunnlagOpt = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(ref.getBehandlingId());
        if (grunnlagOpt.isPresent()) {
            final LocalDate localDate = ref.getUtledetSkjæringstidspunkt();
            return Optional.of(mapTilAggregat(ref.getAktørId(), localDate, grunnlagOpt.get()));
        }
        return Optional.empty();
    }

    @Override
    public PersonopplysningerAggregat hentGjeldendePersoninformasjonPåTidspunkt(Long behandlingId, AktørId aktørId, LocalDate tidspunkt) {
        final Optional<PersonopplysningGrunnlagEntitet> grunnlagOpt = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId);
        if (grunnlagOpt.isPresent()) {
            final Map<Landkoder, Region> landkoderRegionMap = getLandkoderOgRegion(grunnlagOpt.get());
            tidspunkt = tidspunkt == null ? LocalDate.now() : tidspunkt;
            return new PersonopplysningerAggregat(grunnlagOpt.get(), aktørId, DatoIntervallEntitet.fraOgMedTilOgMed(tidspunkt, tidspunkt.plusDays(1)), landkoderRegionMap);
        }
        throw new IllegalStateException("Utvikler feil: Har ikke innhentet opplysninger fra register enda.");
    }

    @Override
    public Optional<PersonopplysningerAggregat> hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(Long behandlingId, AktørId aktørId, LocalDate tidspunkt) {
        final Optional<PersonopplysningGrunnlagEntitet> grunnlagOpt = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId);
        if (grunnlagOpt.isPresent()) {
            final Map<Landkoder, Region> landkoderRegionMap = getLandkoderOgRegion(grunnlagOpt.get());
            tidspunkt = tidspunkt == null ? LocalDate.now() : tidspunkt;
            return Optional.of(new PersonopplysningerAggregat(grunnlagOpt.get(), aktørId, DatoIntervallEntitet.fraOgMedTilOgMed(tidspunkt, tidspunkt.plusDays(1)), landkoderRegionMap));
        }
        return Optional.empty();
    }

    @Override
    public Optional<PersonopplysningerAggregat> hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(Long behandlingId, AktørId aktørId, DatoIntervallEntitet forPeriode) {
        final Optional<PersonopplysningGrunnlagEntitet> grunnlagOpt = getPersonopplysningRepository().hentPersonopplysningerHvisEksisterer(behandlingId);
        if (grunnlagOpt.isPresent()) {
            final Map<Landkoder, Region> landkoderRegionMap = getLandkoderOgRegion(grunnlagOpt.get());
            return Optional.of(new PersonopplysningerAggregat(grunnlagOpt.get(), aktørId, forPeriode, landkoderRegionMap));
        }
        return Optional.empty();
    }

    protected Map<Landkoder, Region> getLandkoderOgRegion(PersonopplysningGrunnlagEntitet grunnlag) {
        final List<Landkoder> landkoder = grunnlag.getRegisterVersjon()
                .map(PersonInformasjonEntitet::getStatsborgerskap)
                .orElse(Collections.emptyList())
                .stream()
                .map(StatsborgerskapEntitet::getStatsborgerskap)
                .collect(toList());
        return behandlingsgrunnlagKodeverkRepository.finnRegionForStatsborgerskap(landkoder);
    }

    protected PersonopplysningerAggregat mapTilAggregat(AktørId aktørId, LocalDate tidspunkt, PersonopplysningGrunnlagEntitet grunnlag) {
        final Map<Landkoder, Region> landkoderRegionMap = getLandkoderOgRegion(grunnlag);
        tidspunkt = tidspunkt == null ? LocalDate.now() : tidspunkt;
        return new PersonopplysningerAggregat(grunnlag, aktørId, DatoIntervallEntitet.fraOgMedTilOgMed(tidspunkt, tidspunkt.plusDays(1)), landkoderRegionMap);
    }

    protected PersonopplysningRepository getPersonopplysningRepository() {
        return personopplysningRepository;
    }

    protected BehandlingsgrunnlagKodeverkRepository getBehandlingsgrunnlagKodeverkRepository() {
        return behandlingsgrunnlagKodeverkRepository;
    }

}
