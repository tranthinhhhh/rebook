package vn.hcmute.rebook.controller.api;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.hcmute.rebook.dto.ProductDto;
import vn.hcmute.rebook.dto.ProductFilter;
import vn.hcmute.rebook.service.ProductService;

/** REST API tra cứu sách (công khai) */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookApiController {
    private final ProductService productService;

    @GetMapping
    public Page<ProductDto> search(@ModelAttribute ProductFilter filter) {
        return productService.search(filter).map(ProductDto::of);
    }

    @GetMapping("/{id}")
    public ProductDto detail(@PathVariable Long id) {
        return ProductDto.of(productService.getVisibleProduct(id));
    }
}
