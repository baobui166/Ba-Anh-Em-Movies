package com.example.movietickets.demo.repository;

import com.example.movietickets.demo.model.ComboFood;
import com.example.movietickets.demo.model.Film;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Repository
@EnableJpaRepositories
public interface FilmRepository extends PagingAndSortingRepository<Film, Long>, JpaRepository<Film, Long> {
    default Page<Film> findAllFilmsForUser(Integer pageNo, Integer pageSize, String sortBy) {
        return findAll(PageRequest.of(pageNo, pageSize, Sort.by(sortBy)));
    }

    @Query("SELECT cf FROM Film cf ORDER BY cf.id DESC")
    List<Film> findAllByOrderByIdDesc();

    @Query("SELECT f FROM Film f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Film> searchFilmByName(@Param("keyword") String keyword);

    List<Film> findByCountry_Id(Long countryId);
    @Query("SELECT f FROM Film f JOIN f.categories c WHERE c.id = :categoryId")
    List<Film> findFilmsByCategoryId(@Param("categoryId") Long categoryId);

    Film findByName(String name);
    @Query("SELECT f FROM Film f WHERE f.id NOT IN (SELECT s.film.id FROM Schedule s)")
    List<Film> findByOpeningdayBeforeAndNotInSchedules();
    @Query("SELECT f FROM Film f WHERE f.openingday > CURRENT_DATE")
    List<Film> findByOpeningdayAfter();
}
